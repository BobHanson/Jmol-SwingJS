/**
 *  JmolMultiTouchDriver.cpp Version 0.3
 *  Author: Bob Hanson, hansonr@stolaf.edu
 *  Date: 12/10/2009
 *
 *  based on HPTouchSmartDriver.cpp
 *  by Andrew Koehring and Jay Roltgen (July 24th, 2009)
 *
 *  This file uses the NextWindow MultiTouch API to receive touch event 
 *  info from the HP TouchSmart computer and send via a socket connection
 *  to the Sparsh-UI Gesture Server.
 *
 *  For use in association with jmol.sourceforge.net using -Msparshui startup option
 *     (see http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol/src/com/sparshui)
 *
 *  NOTE: This driver will not work with the standard Sparsh-UI gesture server, which
 *        expects only a 17-byte message from the device. Here we send 29, which includes
 *        4 bytes for the int message data length and 8 bytes for the ULONLONG event time.
 *
 *  Modifications:
 *
 *  Version 0.2
 *
 *  -- consolidates touchInfos, lastTimes, and touchAlive as "TouchPoint" structure
 *  -- uses SystemTimeToFileTime to generate a "true" event time
 *  -- delivers event time as a 64-bit unsigned integer (ULONGLONG)
 *  -- ignores the NextWindow repetitions at a given location with slop (+/-1 pixel)
 *  -- delivers true moves as fast as they can come (about every 20 ms)
 *  -- times out only for "full death" events (all fingers lifted) after 75 ms
 *  -- automatically bails out if started with a server and later loses service
 *  --  (e.g. applet page closes)
 *  -- operates with or without server for testing
 *  -- not fully inspected for memory leaks or exceptions
 *
 * Version 0.3
 *
 *  -- can only deliver ID "0" or "1"
 *  -- use of bitsets to deliver deaths, then births, then moves
 *     and proper accounting for errors either due to premature deaths or,
 *     in certain circumstances, the NextWindow driver sending a move
 *     with no previous "down" message. This amounts to:
 *        -- canceling all currently active points
 *        -- creating a BIRTH for each of these points in addition to this "moved" point
 *        -- canceling the "move" operation
 *
 *  -- comments in Jmol script format for replaying at any speed within Jmol
 *  -- requires a script file data.spt that would, for example, include:
 *
      function setWidthAndHeight(w, h) {
         screenWidth = w
         screenHeight = h
      }
      function pt(id, index, state, time, x, y) {
          var c = (id == 0 ? "blue", "red")
          y = screenHeight - y
          draw ID @{"p" + id} [@x, @y] color @c
          delay 0.01
      }
 *
 *     thus allowing for playback of the transcript of a session.
 *     
 *
 *  -- uses port 5947 due to nonstandard SparshUI: 
 *
 *   4  (int) -1  (1 point; negative to indicate that per-point message length follows)
 *   4  (int) 21  (21 bytes per point event will be sent)
 *
 *   4  (int) ID  (0 or 1)
 *   4  (float) x (0.0-1.0)
 *   4  (float) y (0.0-1.0)
 *   1  (char) state: 0 (down/BIRTH), 1(up/DEATH), 2(move)
 *   8  (long) time (ms since this driver started)
 *
 *   all ints, floats, and longs in network-endian order
 *
 */

#include "stdafx.h"
#include "math.h"
#include "NWMultiTouch.h"
#include "time.h"
#include <string>


#define VERSION "0.2/Jmol"

// The desired time to wait for more touch move events before sending the touch death.
#define TOUCH_WAIT_TIME_MS 75
#define TOUCH_SLOPXY 1

ULONGLONG startTime;
ULONGLONG timeLast;  // time of last report from NWMultiTouch.dll
ULONGLONG timeThis;  // time to match with timeLast for killing purposes. 
// Flag to stop execution
bool running = true;
bool testing = false; // command line -test flag
bool exitOnDisconnect = true;
bool raw = false; // just report raw output

// bitsets to indicate active points

ULONGLONG bsAlive = 0;

// Socket for sending information to the gesture serve
SOCKET sockfd;
bool useSocket;
bool haveSocket;
// using 5947 because this is nonstandard
#define PORT 5947

// The height and width of the screen in pixels.
int displayWidth, displayHeight;

using namespace std;


// only the part being sent
#define TP_LENGTH 21

// Holds touch point information - this format is compatible with the Sparsh-UI
// gesture server format. (Expanded by Bob Hanson)
struct TouchPoint {

 // for delivery (network endian):

    int _id; 
    float _x;
    float _y;
    ULONGLONG _time;
    char _state;

 // for local use only
    int _index;
    point_t _touchPos;
    ULONGLONG _timeReceived;
    ULONGLONG _timeSent;
};

// Holds the last touch point received on each particular touch ID.
TouchPoint touchPoints[MAX_TOUCHES];
int nextID = 0;
int nActive = 0;

// includes number of points and point length
#define MSG_LENGTH 29

// The type of the touch received from the device.
typedef enum TouchDeviceDataType {
   POINT_BIRTH,
   POINT_DEATH,
   POINT_MOVE,
};

ULONGLONG getTimeNow() {

 // thank you: http://en.allexperts.com/q/C-1040/time-milliseconds-Windows.htm

    SYSTEMTIME st;
    GetSystemTime(&st);
    FILETIME fileTime;
    SystemTimeToFileTime(&st, &fileTime);
    ULARGE_INTEGER uli;
    uli.LowPart = fileTime.dwLowDateTime;
    uli.HighPart = fileTime.dwHighDateTime;
    ULONGLONG systemTimeIn_ms(uli.QuadPart/10000);
    return systemTimeIn_ms;
}

/**
 * Initialize the touch points
 */
void initData() {
    for (int tch = 0; tch < MAX_TOUCHES; tch++) {
        (touchPoints + tch)->_index = tch;
    }
}

bool isAlive(int tch) {
    return ((bsAlive & (1 << tch)) != 0);
}

bool isAlive(TouchPoint *tpp) {
    return isAlive(tpp->_index);
}

void dumpTouchPoint(TouchPoint *tpp) {
    cout << "pt(" << tpp->_id << "," << tpp->_index << "," 
         << (int) tpp->_state << "," << (tpp->_time - startTime) << ","
         << (int) tpp->_touchPos.x  << "," << (int) tpp->_touchPos.y << ","
         << tpp->_x << "," << tpp->_y << ");"
         << "// Active touchpoints [";
    for (int tch = 0; tch < MAX_TOUCHES; tch++)
        if (isAlive(tch))
             cout << " " << tch;
    cout << " ]" << endl;
}

/**
 * Swaps float byte order.
 */
char *swapBytes(float x) {
        union u {
                float f; 
                char temp[4];
        } un, vn;

        un.f = x;
        vn.temp[0] = un.temp[3];
        vn.temp[1] = un.temp[2];
        vn.temp[2] = un.temp[1];
        vn.temp[3] = un.temp[0];
        return vn.temp;
}

/**
 * Swaps long byte order.
 */
char *swapBytes(ULONGLONG x) {
        union u {
                 ULONGLONG f; 
                char temp[8];
        } un, vn;
        un.f = x;
        vn.temp[0] = un.temp[7];
        vn.temp[1] = un.temp[6];
        vn.temp[2] = un.temp[5];
        vn.temp[3] = un.temp[4];
        vn.temp[4] = un.temp[3];
        vn.temp[5] = un.temp[2];
        vn.temp[6] = un.temp[1];
        vn.temp[7] = un.temp[0];
        return vn.temp;
}

/**
 * Send touch point information to the gesture server
 * 
 * @param touchpoint
 *     The touch point we want to send to the Sparsh-UI Gesture Server.
 */
bool sendPoint(TouchPoint *tpp) {

    tpp->_timeSent = tpp->_timeReceived;
    if (testing)
        dumpTouchPoint(tpp);

    if (!haveSocket)
        return true;

    char* buffer = (char*) malloc(MSG_LENGTH); 
    char* bufferptr = buffer;

    // Negative of number of touch points in this packet
    // indicates that we will be sending length of single-touch data as well.
    int temp = htonl(-1);
    memcpy(bufferptr, &temp, 4);
    bufferptr += 4;

    // Length of a single touchpoint data
    temp = htonl(TP_LENGTH);
    memcpy(bufferptr, &temp, 4);
    bufferptr += 4;

    // TouchPoint id
    temp = htonl(tpp->_id);
    memcpy(bufferptr, &temp, 4);
    bufferptr += 4;
        
    // TouchPoint x
    memcpy(bufferptr, swapBytes(tpp->_x), 4);
    bufferptr += 4;

    // TouchPoint y
    memcpy(bufferptr, swapBytes(tpp->_y), 4);
    bufferptr += 4;  

    // TouchPoint state
    memcpy(bufferptr, &(tpp->_state), 1);
    bufferptr++;

    // TouchPoint time
    memcpy(bufferptr, swapBytes(tpp->_time - startTime), 8);

    // Send the touchpoint.
    int nSent = send(sockfd, buffer, MSG_LENGTH, 0); 
    free(buffer);
    if (nSent < 0) {
        // Jmol should automatically restart this.
        cout << "// send failed" << endl;
        if (exitOnDisconnect)
            running = false;
        else
            haveSocket = false;            
        return true;
    }
    return (nSent > 0);
}

/**
 * Initialize the socket connection to the gesture server.
 */

bool initSocket() {
            //Set up socket information
        WSADATA wsaData;
        if(WSAStartup(0x101,&wsaData) != 0) {
                cout << "// Error initializing socket library." << endl;
                return false;
        }
        sockaddr_in inetAddress;
        sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    
        if(sockfd == INVALID_SOCKET) {
            cout << "// invalid socket" << endl;
            return false;
        }
        
        // Set up the socket information
        inetAddress.sin_family = AF_INET;
        inetAddress.sin_port = htons((u_short) (PORT));
        
        unsigned long addr = inet_addr("127.0.0.1");
        hostent* host =  gethostbyaddr((char*) &addr, sizeof(addr), AF_INET);
        inetAddress.sin_addr.s_addr = *((unsigned long*)host->h_addr);
        
        // Connect to the gesture server.
        if(connect(sockfd, (struct sockaddr*) &inetAddress, sizeof(sockaddr)) != 0) {
                cout << "// Connect error -- press ESC to exit or start Jmol with the -Msparshui option" << endl;
                return false;
        }

        // Send the device type.
        const char one = 1;
        int nBytes = send(sockfd, &one, sizeof(char), 0);
        if (nBytes < 1) {
            cout << "// Connection refused" << endl;
            return false;
        }
        cout << "// Connection succeeded" << endl;
        return true;
}


/**
 * adds the NextWindow information to the Converts the point information from the device information to the desired
 * Sparsh-UI information.
 */
void setTouchData(NWTouchPoint* nwtpp, TouchPoint* tpp, ULONGLONG time, char state) {
        tpp->_x = nwtpp->touchPos.x / displayWidth;
        tpp->_y = nwtpp->touchPos.y / displayHeight;
        tpp->_time = time;
        tpp->_state = state;
        tpp->_touchPos.x = nwtpp->touchPos.x;
        tpp->_touchPos.y = nwtpp->touchPos.y;
}

/**
 * Process a touch birth that was received from the device.
 */
void processBirth(NWTouchPoint *nwtpp, TouchPoint *tpp, ULONGLONG time) {
        tpp->_id = nextID;
        nextID = (nextID ? 0 : 1);
        setTouchData(nwtpp, tpp, time, POINT_BIRTH);
        bsAlive |= (1<<tpp->_index);
        sendPoint(tpp);
}


bool checkMove(NWTouchPoint *nwtpp, TouchPoint *tpp) {
        return isAlive(tpp) && (tpp->_state == POINT_BIRTH
            || abs(tpp->_touchPos.x - nwtpp->touchPos.x) > TOUCH_SLOPXY
            || abs(tpp->_touchPos.y - nwtpp->touchPos.y) > TOUCH_SLOPXY);
}

/**
 * Process a touch move that was received from the device.
 */
void processMove(NWTouchPoint *nwtpp, TouchPoint *tpp, ULONGLONG time) {
    if (!checkMove(nwtpp, tpp))
        return;
    setTouchData(nwtpp, tpp, time, POINT_MOVE);
    sendPoint(tpp);
}

/**
 * Process a touch death that was received from the device.
 */
void processDeath(TouchPoint *tpp) {
    if (!isAlive(tpp))
            return; 
    tpp->_state = POINT_DEATH;
    nextID = tpp->_id;
    bsAlive &= ~(1<<tpp->_index);
    sendPoint(tpp);
}

void clearPoints(int bs) {
    bs &= bsAlive;
    for(int tch = 0; tch < MAX_TOUCHES; tch++)
        if (bs & (1<<tch))
            processDeath(touchPoints + tch);
}

NWTouchPoint nwtps[MAX_TOUCHES];

/**
 * Receive the multi-touch information from the device
 */
void __stdcall ReceiveMultiTouchData(DWORD deviceID, DWORD deviceStatus, 
                                                                         DWORD packetID, DWORD touches, 
                                                                         DWORD ghostTouches) {

        timeThis = getTimeNow();
        if (deviceStatus != DS_TOUCH_INFO) {
            if (testing)
                cout << "// NWMultiTouch.dll reports device status " << deviceStatus << endl;
        } else if(touches) {
                ULONGLONG bsBirths = 0;
                ULONGLONG bsDeaths = 0;
                ULONGLONG bsMoves = 0;
                int nBirths = 0;
                int tchMax = 0;
                int tchMin = -1;
                for(int tch = 0; touches && tch < MAX_TOUCHES; tch++) {
                        // "touches" contains a bitmask for present touch ids.
                        // If the bit is set, then a touch with this ID exists.
                        int bit = 1 << tch;
                        if(touches & bit){
                                touches &= ~bit;
                                tchMax = tch;
                                if (tchMin == -1)
                                    tchMin = tch;
                                //Get the touch information.
                                DWORD retCode = GetTouch(deviceID, packetID, nwtps + tch, bit, 0);                                
                                if(retCode == SUCCESS){
                                        TouchPoint *tpp = touchPoints + tch;
                                        tpp->_timeReceived = timeThis;
                                        int state = (nwtps + tch)->touchEventType;
                                        if (raw) {
                                                cout << "received: " << timeThis << " id=" << tch << " " << state
                                                  << " " << (nwtps + tch)->touchPos.x << " " << (nwtps + tch)->touchPos.y << endl;
                                                continue;
                                        }
                                        switch(state){
                                        case TE_TOUCH_DOWN:
                                                bsBirths |= bit;
                                                nBirths++;
                                                break;
                                        case TE_TOUCHING:
                                                if (isAlive(tch)) {
                                                    bsMoves |= bit;
                                                } else {
                                                    // we improperly canceled this one
                                                    // "reboot"
                                                    bsDeaths = -1;
                                                    bsBirths = bsAlive | bsBirths | bsMoves | bit;
                                                }
                                                break;
                                        case TE_TOUCH_UP:
                                                // When there is a full release, we have a problem, because 
                                                // these come only upon the next touch down or move.
                                                // This could be SECONDS or MINUTES later.
                                                // This, I would argue, is a bug in NWMultiTouch.dll.
                                                bsDeaths |= bit;
                                                break;
                                        }
                                }
                        }
                }

                // BH: In my first version it was possible for this driver to have three active
                // touches being reported. Obviously that is not acceptable. This is a 2-touch screen.
                // The problem derives from the fact that the screen can fail to report a kill for
                // reasons unkown to me. 

                // now deliver the events in proper order, clearing IDs, assigning IDs, and moving
                
                if (bsDeaths != 0)
                    clearPoints(bsDeaths);
                        
                int nAlive = 0;                 
                for(int tch = 0; tch < MAX_TOUCHES; tch++)
                    if (isAlive(tch))
                        nAlive++;
                if (nAlive + nBirths > 2) {
                    if (testing)
                        cout << "// Too many touches!" << endl;
                    clearPoints(bsAlive);
                }
                if (bsBirths != 0) {
                    for(int tch = tchMin; tch <= tchMax; tch++)
                        if (bsBirths & (1 << tch)) {
                            processBirth(nwtps + tch, touchPoints + tch, timeThis);
                            processMove(nwtps + tch, touchPoints + tch, timeThis);
                        }
                }
                if (bsMoves != 0) {
                    for(int tch = tchMin; tch <= tchMax; tch++)
                        if (bsMoves & (1 << tch)) {
                            processMove(nwtps + tch, touchPoints + tch, timeThis);
                        }
                }
            }
        timeLast = timeThis;
}

/**
 * Thread responsible for killing touch points promptly after no moves are
 * received for that touch point.
 *
 * Modified for Jmol to only trigger on both fingers away.
 * and onl
 */
DWORD WINAPI TouchKiller(LPVOID lpParam) { 
        Sleep(TOUCH_WAIT_TIME_MS);
        while(running) {
                if (timeLast != timeThis) {
                    // free-wheel if NWMultiTouch.dll is reporting
                    Sleep(5);
                } else {
                    ULONGLONG timeNow = getTimeNow();
                    for (int tch = 0; tch < MAX_TOUCHES; tch++) {
                        if (!isAlive(tch))
                            continue;
                        TouchPoint *tpp = touchPoints + tch;
                        ULONGLONG dt = timeNow - timeLast;//tpp->_timeReceived;
                        if (timeLast != timeThis) {
                            break;
                        } else if (dt > TOUCH_WAIT_TIME_MS) {
                             // Declare dead after about 75 ms.
                             if (timeNow - tpp->_timeSent > (TOUCH_WAIT_TIME_MS<<1)) {
                                 // and update time if not just within 150 ms.
                                 tpp->_time = tpp->_timeReceived;
                             }
                             if (testing)
                                 cout << "// Killing point " << tch << " after " << dt << " ms" << endl;
                             processDeath(tpp);
                        }
                    }
                    Sleep(TOUCH_WAIT_TIME_MS);
                }
        }
        return 0;
}

int main(int argc, char **argv) {

        startTime = getTimeNow();

        cout << "// JmolMultiTouchDriver for the HP TouchSmart Computer Version " << VERSION << endl
             << "// Adapted by Bob Hanson, hansonr@stolaf.edu " << endl
             << "// from HPTouchSmartSparshDriver.cpp, by Andrew Koehring and Jay Roltgen, " << endl
             << "// Ssee http://code.google.com/p/sparsh-ui/" << endl << endl
             << "// Command line options include -test -nosocket -exitondisconnect" << endl << endl
             << "// Jmol script follows. Simply load this data into Jmol using SCRIPT foo.txt" << endl
             << "script data.spt" << endl;
                
        //Get the number of connected devices.
        DWORD numDevices = GetConnectedDeviceCount();
        DWORD serialNum = 0;
        DWORD deviceID = 0;
        initData();
        //testComm();return 0;
        testing = false;
        haveSocket = false;
        useSocket = true;
        exitOnDisconnect = false;
        raw = false;
        for (int i = 1; i < argc; i++) {
            if ((string) argv[i] == "-test") {
                testing = true;
            } else if ((string) argv[i] == "-nosocket") { 
                useSocket = false;
            } else if ((string) argv[i] == "-exitondisconnect") { 
                exitOnDisconnect = true;
            } else if ((string) argv[i] == "-raw") { 
                raw = true;
            }
        }
        cout << "// testing=" << testing << " useSocket=" << useSocket << " exitOnDisconnect=" << exitOnDisconnect << endl;
        
        bool isOK = false;
        if(numDevices > 0) {
                // If we have at least one connected device then try to connect to it.
                // Get the serial number of the device which uniquely identifies the device.
                cout << "// Getting device ID..." << endl;
                serialNum = GetConnectedDeviceID(0);
                
                // Initialize the device, passing in the serial number that uniquely
                // identifies it and the event handler for processing touch packets.
                cout << "// Opening device and registering callback function..." << endl;
                deviceID = OpenDevice(serialNum, &ReceiveMultiTouchData);
                if(deviceID == 1)
                    isOK = true;
                else
                    cout << "// Failed to connect to device, ID:  " << serialNum << endl;
                //SetKalmanFilterStatus(serialNum, true);
        } else {
                cout << "// No valid devices are connected." << endl;
        }

        if (!isOK)
            return 0;

        // Set up the kill thread
        CreateThread(NULL, 0, TouchKiller, NULL, 0, NULL); 

        // Obtain the display info and the resolution.
        NWDisplayInfo displayInfo;
        DWORD retcode = GetConnectedDisplayInfo(0, &displayInfo);
        displayWidth = (int) displayInfo.displayRect.right;
        displayHeight = (int) displayInfo.displayRect.bottom;
        cout << "setWidthHeight(" << displayWidth << "," << displayHeight << ");" << endl;

        DWORD displayMode = RM_MULTITOUCH; // same as RM_SLOPESMODE ?
        SetReportMode(deviceID, displayMode);

        if (useSocket)
            haveSocket = initSocket();
        if (useSocket && !haveSocket && exitOnDisconnect) {
            cout << "// No socket - exiting. Use -nosocket to avoid exiting" << endl;
            return 1;
        }

        cout << "// Press ESC to Quit or I to re-initialize socket" << endl;
        while(running) {

            if (useSocket && !haveSocket) {
                cout << "// No socket and no -nosocket or -testnosocket -- waiting for server -- press ESC to exit" << endl;
                while(!haveSocket && running) {
                    if (_kbhit()) {
                        if (_getch()==0x1B)         
                            running = false;
                    }
                    if (running) {
                        WSACleanup();
                        haveSocket = initSocket();
                        Sleep(1000);
                    }
                }
            }
            if (_kbhit()) {
                if (_getch()==0x1B)         
                    running = false;
                else if (_getch()==0x49) {
                    WSACleanup();
                    haveSocket = initSocket();
                }
            }
            if (running)
                Sleep(200);
        }
        
        cout << "// Closing connection to device..." << endl;

        // Close any open devices.
        CloseDevice(serialNum);
        
        //Reset the device connect/disconnect event handlers.
        SetConnectEventHandler(NULL);
        SetDisconnectEventHandler(NULL);

        return 0;
}
