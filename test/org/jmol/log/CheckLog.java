package org.jmol.log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class CheckLog {

  public static void main(String[] args) {
    String f = (args.length == 0 ? "c:/temp/logJmol.txt" : args[0]);
    Date d = new Date("2/06/2024");
    long l = d.getTime();
    System.out.println(d);
    int itemCount = -1;
    int day = 0;
    try (FileInputStream fis = new FileInputStream(f)) {
      try (FileOutputStream fos = new FileOutputStream(f + ".csv")) {
        byte[] item = new byte[4];
        int n = 0;
        int count = 0;
        int hourlast = 999;
        do {
          n = fis.read(item);
          itemCount++;
          int hour = item[0] - '0';
          if (n < 0 || hour < hourlast) {
            day++;
            String s = day + "," + count + "\r\n";
            fos.write(s.getBytes());
            count = 0;
          }
          hourlast = hour;    
          count++;
        } while (n > 0);
      } catch (IOException e) {
      }
    } catch (IOException e) {
    }
    System.out.println(itemCount + " " + new Date(l - 24l*60*60*1000*day) + " " + (int)(itemCount/day));
    System.exit(0);

  }

}
