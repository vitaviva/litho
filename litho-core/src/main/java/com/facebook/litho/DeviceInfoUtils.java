/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.os.Build;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DeviceInfoUtils {

  /**
   * The default return value of any method in this class when an error occurs or when processing
   * fails (Currently set to -1). Use this to check if the information about the device in question
   * was successfully obtained.
   */
  public static final int DEVICEINFO_UNKNOWN = -1;

  /**
   * Reads the number of CPU cores from the first available information from {@code
   * /sys/devices/system/cpu/possible}, {@code /sys/devices/system/cpu/present}, then {@code
   * /sys/devices/system/cpu/}.
   *
   * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
   */
  public static int getNumberOfCPUCores() {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
      // Gingerbread doesn't support giving a single application access to both cores, but a
      // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
      // chipset and Gingerbread; that can let an app in the background run without impacting
      // the foreground application. But for our purposes, it makes them single core.
      return 1;
    }

    int cores;
    try {
      cores = getCoresFromFileInfo("/sys/devices/system/cpu/possible");
      if (cores == DEVICEINFO_UNKNOWN) {
        cores = getCoresFromFileInfo("/sys/devices/system/cpu/present");
      }
      if (cores == DEVICEINFO_UNKNOWN) {
        cores = getCoresFromCPUFileList();
      }
    } catch (SecurityException e) {
      cores = DEVICEINFO_UNKNOWN;
    } catch (NullPointerException e) {
      cores = DEVICEINFO_UNKNOWN;
    }

    return cores;
  }

  /**
   * Tries to read file contents from the file location to determine the number of cores on device.
   *
   * @param fileLocation The location of the file with CPU information
   * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
   */
  private static int getCoresFromFileInfo(String fileLocation) {
    InputStream is = null;
    try {
      is = new FileInputStream(fileLocation);
      BufferedReader buf = new BufferedReader(new InputStreamReader(is));
      String fileContents = buf.readLine();
      buf.close();
      return getCoresFromFileString(fileContents);
    } catch (IOException e) {
      return DEVICEINFO_UNKNOWN;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Do nothing.
        }
      }
    }
  }

  /**
   * Converts from a CPU core information format to number of cores.
   *
   * @param str The CPU core information string, in the format of "0-N"
   * @return The number of cores represented by this string
   */
  static int getCoresFromFileString(String str) {
    if (str == null || !str.matches("0-[\\d]+$")) {
      return DEVICEINFO_UNKNOWN;
    }
    int cores = Integer.valueOf(str.substring(2)) + 1;
    return cores;
  }

  private static int getCoresFromCPUFileList() {
    return new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
  }

  private static final FileFilter CPU_FILTER =
      new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          String path = pathname.getName();
          // regex is slow, so checking char by char.
          if (path.startsWith("cpu")) {
            for (int i = 3; i < path.length(); i++) {
              if (!Character.isDigit(path.charAt(i))) {
                return false;
              }
            }
            return true;
          }
          return false;
        }
      };
}
