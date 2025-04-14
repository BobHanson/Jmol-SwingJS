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

import io.github.dan2097.jnainchi.inchi.InchiLibrary.tagINCHIBondStereo2D;

public enum InchiBondStereo {
  
  /** No stereo information recorded for this bond */
  NONE(tagINCHIBondStereo2D.INCHI_BOND_STEREO_NONE),
  
  /**sharp end points to this atom i.e. reference atom is {@link InchiBond#getStart()} */
  SINGLE_1UP(tagINCHIBondStereo2D.INCHI_BOND_STEREO_SINGLE_1UP),
  
  /**sharp end points to this atom i.e. reference atom is {@link InchiBond#getStart()} */
  SINGLE_1EITHER(tagINCHIBondStereo2D.INCHI_BOND_STEREO_SINGLE_1EITHER),
  
  /**sharp end points to this atom i.e. reference atom is {@link InchiBond#getStart()} */
  SINGLE_1DOWN(tagINCHIBondStereo2D.INCHI_BOND_STEREO_SINGLE_1DOWN),

  /**sharp end points to the opposite atom i.e. reference atom is {@link InchiBond#getEnd()} */
  SINGLE_2UP(tagINCHIBondStereo2D.INCHI_BOND_STEREO_SINGLE_2UP),
  
  /**sharp end points to the opposite atom i.e. reference atom is {@link InchiBond#getEnd()} */
  SINGLE_2EITHER(tagINCHIBondStereo2D.INCHI_BOND_STEREO_SINGLE_2EITHER),
  
  /**sharp end points to the opposite atom i.e. reference atom is {@link InchiBond#getEnd()} */
  SINGLE_2DOWN(tagINCHIBondStereo2D.INCHI_BOND_STEREO_SINGLE_2DOWN),
  
  /** unknown stereobond geometry*/
  DOUBLE_EITHER(tagINCHIBondStereo2D.INCHI_BOND_STEREO_DOUBLE_EITHER);
  
  private final byte code;
  
  private InchiBondStereo(int code) {
    this.code = (byte) code;
  }
  
  byte getCode() {
    return code;
  }
  
  private static final Map<Object, InchiBondStereo> map = new HashMap<>();
  
  static {
    for (InchiBondStereo val : InchiBondStereo.values()) {
      map.put(Byte.valueOf(val.code), val);
      map.put(val.name().toLowerCase(Locale.ROOT), val);
    }
  }
    
  public static int getCodeObj(Object val) {
    if (val != null) {
      InchiBondStereo e = (val instanceof InchiBondStereo ? (InchiBondStereo) val 
          : map.get(val.toString().toLowerCase(Locale.ROOT)));
      if (e != null)
        return e.getCode();
    }
    return NONE.getCode();
  }
  
  static InchiBondStereo of(byte code) {
    return map.get(code);
  }

}
