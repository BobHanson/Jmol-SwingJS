/******************************************************************************
**                                                                           **
**  NWMultiTouch.h - Header file.                                            **
**  Author : Jim Gardner                                                     **
**  Date : 22/01/2008                                                        **
**  Version : 1.2.3.0 Beta with Gesture Decoder                              **
**                                                                           **
**  This header file defines the public functions / enumerations exported    **
**  from NWMultiTouch.dll for use as part of the NextWindow MultiTouch API.  **
**                                                                           **
**  Copyright (c) 2008, NextWindow Ltd.                                      **
**  All rights reserved.                                                     **
**                                                                           **
**  Redistribution and use in source and binary forms, with or without       **
**  modification, are permitted provided that the following conditions       **
**  are met:                                                                 **
**      * Redistributions of source code must retain the above copyright     **
**        notice, this list of conditions and the following disclaimer.      **
**      * Redistributions in binary form must reproduce the above copyright  **
**        notice, this list of conditions and the following disclaimer in    **
**        the documentation and/or other materials provided with the         **
**        distribution.                                                      **
**      * Neither the name of NextWindow nor the names of its contributors   **
**        may be used to endorse or promote products derived from this       **
**        software without specific prior written permission.                **
**                                                                           **
**  THIS SOFTWARE IS PROVIDED BY NEXTWINDOW LTD ``AS IS'' AND ANY            **
**  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE        **
**  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR       **
**  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL NEXTWINDOW LTD BE LIABLE FOR   **
**  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL   **
**  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS  **
**  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)    **
**  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,      **
**  STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN **
**  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE          **
**  POSSIBILITY OF SUCH DAMAGE.                                              **
**                                                                           **
*******************************************************************************/


#ifndef  __NWMULTITOUCH_H__
	#define __NWMULTITOUCH_H__

extern "C"
{

#include <time.h>
#include <windows.h>

#define NUM_CAMS            2
#define NUM_PACKETS_STORED  255
#define MAX_TOUCHES         32

//Define Success/Error codes.
typedef enum
{
	SUCCESS                    = 1,
	ERR_DEVICE_NOT_OPEN        = -1,
	ERR_INVALID_PACKET_ID      = -2,
	ERR_INVALID_TOUCH_ID       = -3,
	ERR_TOO_MANY_TOUCHES       = -4,
	ERR_DEVICE_DOES_NOT_EXIST  = -5,
  ERR_DISPLAY_DOES_NOT_EXIST = -6,
  ERR_FUNCTION_NOT_SUPPORTED = -7,
  ERR_INVALID_SENSOR_NUMBER  = -8,
  ERR_SLOPES_MODE_NOT_SUPPORTED = -9
} successCode_t;

//Touch Event Type enumeration.
typedef enum
{
	TE_TOUCH_DOWN = 1,
	TE_TOUCHING   = 2,
	TE_TOUCH_UP   = 3
}	touchEventType_t;

//Device Status Enumeration
typedef enum
{
	DS_CONNECTED    = 1,
	DS_TOUCH_INFO   = 2,
	DS_DISCONNECTED = 3
} deviceStatus_t;

//Touch Type enumeration
typedef enum
{
	TT_TOUCH      = 1,
	TT_GHOSTTOUCH = 2,
	TT_CENTROID   = 3
} touchType_t;

//Report Mode enumeration
typedef enum
{
  RM_NONE       = 0,
  RM_MOUSE      = 1,
  RM_MULTITOUCH = 2,
  RM_DIGITISER  = 4,
  RM_SLOPESMODE = 8
} reportMode_t;

//Event handler function definitions
//Touch event handler function definition.
typedef void (__stdcall *NWReceiveTouchInfo)(DWORD deviceID, DWORD deviceStatus, DWORD packetID, DWORD touches, DWORD ghostTouches);

//Connection event handler function definition.
typedef void (__stdcall *NWConnectEvent)(DWORD deviceID);

//Disconnection event handler function definition.
typedef void (__stdcall *NWDisconnectEvent)(DWORD deviceID);


//Define the point structure.
typedef struct
{
  float x;
  float y;
} point_t;

//Define the rect structure.
typedef struct
{
  float left;
  float top;
  float right;
  float bottom;
} rect_t;

//Define the NWDeviceInfo structure.
typedef struct
{
  DWORD	serialNumber;
  DWORD	modelNumber;
  DWORD	firmwareVersionMajor;
  DWORD	firmwareVersionMinor;
  DWORD	productID;
  DWORD	vendorID;
} NWDeviceInfo;

//Define the NWDisplayInfo structure
typedef struct
{
  DWORD   deviceNo;
  rect_t  displayRect;
  rect_t  displayWorkRect;
  bool    isPrimary;
  char    deviceName[CCHDEVICENAME];
} NWDisplayInfo;

//Define the NWTouchPoint structure.
typedef struct
{
  DWORD       touchID;
  DWORD       touchType;
  __int64     touchStart;
  point_t     touchPos;
  float       velocity;
  float       acceleration;
  float       touchArea;
  DWORD       touchEventType;
  DWORD       confidenceLevel;
  float       height;
  float       width;
} NWTouchPoint;

//Connection functions.
DWORD __declspec(dllexport) __cdecl OpenDevice(DWORD deviceID, NWReceiveTouchInfo callbackFn);
void __declspec(dllexport) __cdecl CloseDevice(DWORD deviceID);

//Sets the event handlers for receiving OnConnect/OnDisconnect events.
void __declspec(dllexport) __cdecl SetConnectEventHandler (NWConnectEvent cHandler);
void __declspec(dllexport) __cdecl SetDisconnectEventHandler (NWDisconnectEvent dHandler);

//Getting/Setting Report Mode functions
DWORD __declspec(dllexport) __cdecl SetReportMode (DWORD deviceID, DWORD reportMode);
DWORD __declspec(dllexport) __cdecl GetReportMode (DWORD deviceID);

//Sensor Position functions
DWORD __declspec(dllexport) __cdecl SetSensorPosition (DWORD deviceID, DWORD camNum, point_t position);
DWORD __declspec(dllexport) __cdecl GetSensorPosition (DWORD deviceID, DWORD camNum, point_t* position);

//Near Field function - only available for firmware versions 2.98 and above.
DWORD __declspec(dllexport) __cdecl GetNearFieldRegion (DWORD deviceID, point_t* nearField);
DWORD __declspec(dllexport) __cdecl SetNearFieldRegion (DWORD deviceID, point_t nearField);

//Device Enumeration functions.
DWORD __declspec(dllexport) __cdecl GetConnectedDeviceID (DWORD deviceNo);
DWORD __declspec(dllexport) __cdecl GetConnectedDeviceCount ();

//Display Enumeration functions.
DWORD __declspec(dllexport) __cdecl GetConnectedDisplayInfo(int displayNo, NWDisplayInfo* displayInfo);
DWORD __declspec(dllexport) __cdecl GetConnectedDisplayCount();

//Touch functions.
DWORD __declspec(dllexport) __cdecl GetTouch (DWORD deviceID, DWORD packetID, NWTouchPoint* touchPoint, DWORD touch, DWORD ghostTouch);
DWORD __declspec(dllexport) __cdecl GetCentroid (DWORD deviceID, DWORD packetID, NWTouchPoint* touchPoint, DWORD touches, DWORD ghostTouches);
DWORD __declspec(dllexport) __cdecl GetPolygon (DWORD deviceID, DWORD packetID, DWORD touch, DWORD ghostTouch, point_t* pointArray, DWORD size);

//Measurement functions.
float __declspec(dllexport) __cdecl GetAngleBetweenPointsDegrees (point_t point1, point_t point2);
float __declspec(dllexport) __cdecl GetAngleBetweenPointsRadians (point_t point1, point_t point2);
float __declspec(dllexport) __cdecl GetDistanceBetweenPoints(point_t point1, point_t point2);

//Units Conversion Functions.
DWORD __declspec(dllexport) __cdecl SetTouchScreenDimensions (DWORD deviceID, float xMin, float yMin, float xMax, float yMax);
DWORD __declspec(dllexport) __cdecl GetTouchScreenDimensions (DWORD deviceID, float* xMin, float* yMin, float* xMax, float* yMax);

//Kalman Filter Control functions
void __declspec(dllexport) __cdecl SetKalmanFilterCoefficients(DWORD deviceID, float g, float h, float k);
void __declspec(dllexport) __cdecl SetKalmanFilterStatus(DWORD deviceID, bool kalmanOn);

//Device Information
DWORD __declspec(dllexport) __cdecl GetTouchDeviceInfo (DWORD deviceID, NWDeviceInfo* deviceInfo);

//Get/Set Loopback Status functions
DWORD __declspec(dllexport) __cdecl GetDeviceLoopbackStatus (DWORD deviceID);
DWORD __declspec(dllexport) __cdecl SetDeviceLoopbackStatus (DWORD deviceID, bool status);

/******************************************************************************
**                                                                           **
**  NWMultiTouch.h - Gesture Decoder with Loop-back.                         **
**  Author : Keith Colson                                                    **
**  Date : 06/010/2008                                                       **
**  This is an extension of the mutli-touch DLL                              **
**  It gives a much simpler access to the multi touch gestures               **
**                                                                           **
*******************************************************************************/
// ******************** MultiTouch Gesture EventTypes **************************
typedef enum
{
	MTG_THRESHOLD = -1,	// used internally only
	MTG_NONE = 0,			 	// no gesture (invalid)
	MTG_TOUCHDOWN = 1,  // Two touches are down
	MTG_LIFTOFF = 2,    // Two touches lift off
	MTG_SCROLLH  = 3,   // Horizontal scroll increment/decrement
	MTG_SCROLLV = 4,    // Vertical scroll increment/decrement
	MTG_PINCH = 5,      // pinch/stretch increment/decrement
	MTG_TAP= 6,	        // generated when a down follows an up = click
	MTG_DOUBLETAP = 7,  // two fingers tapped the screen twice
	MTG_ROTATE = 8,     // not implemented yet
}	MultiTouchGestureEventTypes_t;

/*******************************************************************************
* *ONGESTURE is the Gesture event callback function pointer prototype/typedef  *
* Usage: void __stdcall OnGestureEvent(int tGesture, int tAmountRelative, etc){*
*                                                                              *
*******************************************************************************/
typedef void (__stdcall *ONGESTURE)(int tGesture, int tAmountRelative, float tX, float tY, unsigned int tElapsed_ms);

/*******************************************************************************
* SetGestureMode sets the gesture mode on or off		                           *
*      Usage: ditiser mode is looped back (ditiser mode supported ONLY)        *
* int tResult = SetGestureMode (tDeviceID, RM_DIGITISER, &OnGestureEvent);     *
*******************************************************************************/
int __declspec(dllexport) __cdecl SetGestureMode(int tDeviceID, int tMode, ONGESTURE tCallbackPtr);

/*******************************************************************************
* This sets the threshold at which the gesture activates from the touch down   *
*      Usage: SetGestureThreshold (MTG_SCROLLH, 5); set hscroll to 5%          *
* e.g hscroll gesture will start when the touches move 5% from touch down      *
*******************************************************************************/
void __declspec(dllexport) __cdecl SetGestureThreshold(int GestureType, float tPercent);

/*******************************************************************************
* SetGestureGain sets the gain of the gesture (smaller % is faster acting)     *
*      Usage: SetGestureGain (MTG_SCROLLH, 5); set hscroll to 5% increments    *
* e.g hscroll event happens every time two fingers move 5% of the screen width *
*******************************************************************************/
void __declspec(dllexport) __cdecl SetGestureGain(int GestureType, float tPercentGain);

/*******************************************************************************
* SetDisplayRect sets the display region to be used in pixels (for gestures)   *
*      Usage: SetDisplayRect (rect filled with 0, 0, 1024, 768)                *
*             this can point to the second monitor too, e.g  1024,0,2047,768   *
*             x,y gesture coordinates that arrive use these dimensions         *
*******************************************************************************/
void __declspec(dllexport) __cdecl SetDisplayRect (rect_t tDisplayRect);



}   //End Extern "C"

#endif  // __NWMULTITOUCH_H__
