/* Copyright (c) 2002-2010 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'JDXDecompressor.java' - 
// University of the West Indies, Mona Campus
//
// 23-08-2010 fix for DUP before DIF e.g. at start of line

package jspecview.source;

/**
 * @author Bob Hanson - hansonr@stolaf.edu
 */
public class JDXDecompressorTest {

  
  private static String[] testLines = new String[] {
      "1JT%jX",
      "D7m9jNOj9RjLmoPKLMj4oJ8j7PJT%olJ3MnJj2J0j7MQpJ9j3j0TJ0J2j3PKmJ2KJ4Ok2J4Mk",
      "A4j5lqkJ4rNj0J6j3JTpPqPNj6K0%j1J1lnJ8k3Pj1%J3j8J6J2j0J%Jj1Pkj1RJ5nj2OnjJJ3",
      "Ak8K1MPj4Nj2RJQoKnJ0j8J5mQl4L0j5J7k2NJMTJ1noLNj0KkqLmJ7Lk3MJ7p%qoLJ3nRjoJQ",
      "b2TJ2j7Nj0J8jpLOlOj2Jj0RJ5pmqJ1lpJ1pPjKJjkPj2MjJ2j2Qj2k3L4qnJ4prmRKmoJ6J5r",
      "I82314Q00sQ01Q00S2J85%W3A000100"
    };
  private static double[][] testResults = new double[][] {
    new double[] {
        1.0,2.0,3.0,3.0,
        2.0,1.0,0.0,-1.0,-2.0,
        -3.0,
        },
        new double[] {
        47.0,-2.0,-3.0,2.0,
        8.0,-11.0,-2.0,-3.0,0.0,
        -4.0,-10.0,-3.0,-1.0,2.0,
        6.0,-8.0,-14.0,4.0,-13.0,
        -6.0,-5.0,-4.0,-4.0,-10.0,
        -13.0,0.0,4.0,-1.0,0.0,
        -12.0,-2.0,-19.0,-15.0,-7.0,
        -14.0,5.0,-8.0,-18.0,-28.0,
        -18.0,-6.0,-19.0,-12.0,-10.0,
        -14.0,-2.0,0.0,14.0,20.0,
        -2.0,12.0,16.0,14.0,
        },
        new double[] {
        14.0,-1.0,-4.0,-12.0,
        -14.0,0.0,-9.0,-4.0,-14.0,
        2.0,-11.0,-10.0,-9.0,-16.0,
        -9.0,-17.0,-10.0,-5.0,-21.0,
        -1.0,-1.0,-12.0,-1.0,-4.0,
        -9.0,9.0,-14.0,-7.0,-18.0,
        -18.0,-5.0,-23.0,-7.0,5.0,
        -5.0,-4.0,-4.0,-3.0,-14.0,
        -7.0,-9.0,-20.0,-11.0,4.0,
        -1.0,-13.0,-7.0,-12.0,-13.0,
        -12.0,1.0,
        },
        new double[] {
        1.0,-27.0,-6.0,-2.0,
        5.0,-9.0,-4.0,-16.0,-7.0,
        -6.0,2.0,-4.0,-2.0,-7.0,
        3.0,-15.0,0.0,-4.0,4.0,
        -30.0,0.0,-15.0,2.0,-20.0,
        -15.0,-14.0,-10.0,-6.0,5.0,
        0.0,-6.0,-3.0,2.0,-8.0,
        -6.0,-8.0,-16.0,-13.0,-17.0,
        0.0,3.0,-20.0,-16.0,1.0,
        -6.0,-6.0,-14.0,-20.0,-17.0,
        -4.0,-9.0,0.0,-1.0,-7.0,
        -6.0,2.0,
        },
        new double[] {
        -22.0,-22.0,-10.0,-27.0,
        -22.0,-32.0,-14.0,-15.0,-22.0,
        -19.0,-13.0,-16.0,-10.0,-22.0,
        -21.0,-31.0,-22.0,-7.0,-14.0,
        -18.0,-26.0,-15.0,-18.0,-25.0,
        -14.0,-21.0,-14.0,-15.0,-13.0,
        -12.0,-13.0,-15.0,-8.0,-20.0,
        -16.0,-17.0,-5.0,-17.0,-9.0,
        -21.0,-44.0,-10.0,-18.0,-23.0,
        -9.0,-16.0,-25.0,-29.0,-20.0,
        -18.0,-22.0,-28.0,-12.0,3.0,
        -6.0,
        },
        new double[] {
        982314.0,983114.0,983914.0,984714.0,
        985514.0,986314.0,987114.0,987914.0,988714.0,
        989514.0,990315.0,991115.0,991915.0,992715.0,
        993515.0,994315.0,995115.0,995915.0,996715.0,
        997515.0,998315.0,999115.0,999915.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,1000100.0,
        1000100.0,1000100.0,1000100.0,1000100.0,
        },
     };

  private static double dx5 = (1.287944E+03-3.500640E+03)/(2000-1);
  private static double[][] testData = new double[][] {
    new double[] {0, 1, 10, -3}, 
    new double[] {8191, -1, 8139, 14},
    new double[] {8139, -1, 8089, 1},
    new double[] {8089, -1, 8034, 2},
    new double[] {6142, -1, 6088, -9},
    new double[] {1374.2821, dx5, 1287.9438 - dx5, 1000100}, 
    };

  private static int testIndex = 5;
  
  public static void main(String[] args) {
    
//    for (int i = 0; i < 6; i++) {
//      testCreate(i);
//    }

    for (int i = 0; i < 6; i++) {
      testRun(i);
    }
    

//	  testDemo(args);
  }

  private static void testRun(int t) {
    JDXDecompressor d = new JDXDecompressor(testLines[t], 0);
    int i = 0;
    while(d.hasNext()) {
      double v = d.next().doubleValue();
      double vr = testResults[t][i++];
      boolean isOK = (v == vr);
      if (!isOK) {
        System.err.println ("test " + t + " failed " + i + ": " + v + " != " + vr);
        return;
      }
    }
    System.out.println("test " + t + " OK");
  }

  static void testCreate(int t) {
    JDXDecompressor d = new JDXDecompressor(testLines[t], 0);
    int n = 1;
    System.out.println("new double[] {");
    while(d.hasNext()) {
      System.out.print(d.next() + ",");
      if ((++n % 5) == 0)
        System.out.println();
    }
    System.out.println("\n},");
  }

  static void testDemo(String[] args) {
    String line;
    double x, xexp, dx, yexp;
    if (args.length == 0) {
      line = testLines[testIndex];
      double[] data = testData[testIndex];
      x = data[0];
      dx = data[1];
      xexp = data[2];
      yexp = data[3];
      
    } else {
      line = args[0];
      x = 0;
      dx = 1;
      xexp = -1;
      yexp = Double.NaN;
    }
    JDXDecompressor d = new JDXDecompressor(line, 0);
    int n = 0;
    while(d.hasNext()) {
      String s = line.substring(d.ich);
      if (n > 0)
        x += dx;
      System.out.println(
          (++n) + " " + 
          x + " " + 
              d.next() + " " + s);
    }
    if (xexp >= 0)
      System.out.println("expected x " + xexp + " final x " + x + " expected y "  + yexp + " final y " + d.lastY);
  }
}
