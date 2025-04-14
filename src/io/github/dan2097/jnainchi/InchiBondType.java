/**
 * JNA-InChI - Library for calling InChI from Java
 * Copyright © 2018 Daniel Lowe
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.dan2097.jnainchi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.dan2097.jnainchi.inchi.InchiLibrary.tagINCHIBondType;

public enum InchiBondType {

  NONE(tagINCHIBondType.INCHI_BOND_TYPE_NONE),
  
  SINGLE(tagINCHIBondType.INCHI_BOND_TYPE_SINGLE),
  
  DOUBLE(tagINCHIBondType.INCHI_BOND_TYPE_DOUBLE),
  
  TRIPLE(tagINCHIBondType.INCHI_BOND_TYPE_TRIPLE),
  
  /** avoid by all means */
  ALTERN(tagINCHIBondType.INCHI_BOND_TYPE_ALTERN);
  
  private final byte code;
  
  private InchiBondType(int code) {
    this.code = (byte) code;
  }

  byte getCode() {
    return code;
  }
  
  private static final Map<Object, InchiBondType> map = new HashMap<>();
  
  static {
    for (InchiBondType val : InchiBondType.values()) {
      map.put(Byte.valueOf(val.code), val);
      map.put(val.name().toLowerCase(Locale.ROOT), val);
    }
  }

  public static int getCodeObj(Object val) {
    if (val != null) {
      InchiBondType e = (val instanceof InchiBondType ? (InchiBondType) val 
          : map.get(val.toString().toLowerCase(Locale.ROOT)));
      if (e != null)
        return e.getCode();
    }
    return SINGLE.getCode();
  }

  public static InchiBondType of(byte code) {
    return map.get(code);
  }
}
