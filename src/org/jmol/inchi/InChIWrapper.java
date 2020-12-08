/*
 * 
 * From inchi-api.h
 * 
 * Copyright 2006-2011 Sam Adams <sea36 at users.sourceforge.net>
 *
 * This file is part of JNI-InChI.
 *
 * JNI-InChI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License; or
 * (at your option) any later version.
 *
 * JNI-InChI is distributed in the hope that it will be useful;
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JNI-InChI.  If not; see <http://www.gnu.org/licenses/>.
 */
package org.jmol.inchi;

import org.jmol.inchi.InChIC.inchi_Input;
import org.jmol.inchi.InChIC.inchi_Output;

import net.sf.jniinchi.JniInchiInput;
import net.sf.jniinchi.JniInchiInputData;
import net.sf.jniinchi.JniInchiOutput;
import net.sf.jniinchi.JniInchiOutputKey;
import net.sf.jniinchi.JniInchiOutputStructure;

/**
 * From JniInchiWrapper.c -- testing only
 */
public class InChIWrapper {

  
//  public static JniInchiOutput GetINCHI(JniInchiInput input) {
//
//  inchi_input = new inchi_Input();
//  inchi_Output inchi_output = new inchi_Output();
//  int r;
//
//  if (0 == initInchiInput(inchi_input, input)) {
//      return null;
//  }
//
//  JniInchiOutput ret = GetINCHI(inchi_input, inchi_output);
//
//  JniInchiOutput output = getInchiOutput(ret, inchi_output);
//
//  FreeINCHI(inchi_output);
//  free(inchi_input.szOptions);
//  Free_inchi_Input(&inchi_input);
//
//  return output;
//}
//
//  
//  
//  
//
//  int initInchiInput(JNIEnv *env, inchi_Input *inchi_input, jobject input) {
//
//    int i;
//    inchi_Atom *atoms;
//    inchi_Stereo0D *stereos;
//    jstring joptions;
//    const char *options;
//    char *opts;
//
//    jboolean iscopy = JNI_TRUE;
//
//    jint natoms = (*env)->CallIntMethod(env, input, getNumAtoms);
//    jint nstereo = (*env)->CallIntMethod(env, input, getNumStereo0D);
//    jint nbonds = (*env)->CallIntMethod(env, input, getNumBonds);
//
//    if (natoms > MAX_ATOMS) {
//        (*env)->ThrowNew(env, IllegalArgumentException, "Too many atoms");
//        return 0;
//    }
//
//    atoms = malloc(sizeof(inchi_Atom) * natoms);
//    memset(atoms,0,sizeof(inchi_Atom) * natoms);
//
//    for (i = 0; i < natoms; i++) {
//
//        jobject atom = (*env)->CallObjectMethod(env, input, getAtom, i);
//
//        inchi_Atom *iatom = &atoms[i];
//
//        jstring jelname = (jstring) (*env)->CallObjectMethod(env, atom, getElementType);
//        const char *elname = (*env)->GetStringUTFChars(env, jelname, &iscopy);
//        if (strlen(elname) > ATOM_EL_LEN) {
//            (*env)->ThrowNew(env, IllegalArgumentException, "Element name too long; maximum: " + ATOM_EL_LEN);
//            (*env)->ReleaseStringUTFChars(env, jelname, elname);
//            return 0;
//        }
//        strcpy(iatom->elname, elname);
//        (*env)->ReleaseStringUTFChars(env, jelname, elname);
//
//        iatom->x = (*env)->CallDoubleMethod(env, atom, getX);
//        iatom->y = (*env)->CallDoubleMethod(env, atom, getY);
//        iatom->z = (*env)->CallDoubleMethod(env, atom, getZ);
//
//        iatom->charge = (*env)->CallIntMethod(env, atom, getCharge);
//        iatom->radical = (*env)->CallIntMethod(env, atom, getRadical);
//
//        iatom->num_iso_H[0] = (*env)->CallIntMethod(env, atom, getImplicitH);
//        iatom->num_iso_H[1] = (*env)->CallIntMethod(env, atom, getImplicitProtium);
//        iatom->num_iso_H[2] = (*env)->CallIntMethod(env, atom, getImplicitDeuterium);
//        iatom->num_iso_H[3] = (*env)->CallIntMethod(env, atom, getImplicitTritium);
//
//        iatom->isotopic_mass = (*env)->CallIntMethod(env, atom, getIsotopicMass);
//
//        iatom->num_bonds = 0;
//    }
//
//
//    for (i = 0; i < nbonds; i++) {
//
//        jobject bond = (*env)->CallObjectMethod(env, input, getBond, i);
//        jobject atomO = (*env)->CallObjectMethod(env, bond, getOriginAtom);
//        jobject atomT = (*env)->CallObjectMethod(env, bond, getTargetAtom);
//        jint bondType = (*env)->CallIntMethod(env, bond, getBondType);
//        jint bondStereo = (*env)->CallIntMethod(env, bond, getBondStereo);
//
//        jint iaO = (*env)->CallIntMethod(env, input, getAtomIndex, atomO);
//        jint iaT = (*env)->CallIntMethod(env, input, getAtomIndex, atomT);
//
//
//        inchi_Atom *iatom = &atoms[iaO];
//        int numbonds = iatom->num_bonds;
//        if (numbonds >= MAXVAL) {
//            free(atoms);
//            (*env)->ThrowNew(env, IllegalArgumentException, "Too many bonds from one atom; maximum: " + MAXVAL);
//            return 0;
//        }
//        iatom->neighbor[numbonds] = iaT;
//        iatom->bond_type[numbonds] = bondType;
//        iatom->bond_stereo[numbonds] = bondStereo;
//        iatom->num_bonds++;
//
//    }
//
//    stereos = malloc(sizeof(inchi_Stereo0D) * nstereo);
//    memset(stereos,0,sizeof(inchi_Stereo0D) * nstereo);
//
//    for (i = 0; i < nstereo; i++) {
//
//        jobject stereo = (*env)->CallObjectMethod(env, input, getStereo0D, i);
//
//        inchi_Stereo0D *istereo = &stereos[i];
//
//        jobject cat = (*env)->CallObjectMethod(env, stereo, getCentralAtom);
//        jobject nat0 = (*env)->CallObjectMethod(env, stereo, getNeighbor, 0);
//        jobject nat1 = (*env)->CallObjectMethod(env, stereo, getNeighbor, 1);
//        jobject nat2 = (*env)->CallObjectMethod(env, stereo, getNeighbor, 2);
//        jobject nat3 = (*env)->CallObjectMethod(env, stereo, getNeighbor, 3);
//
//        istereo->central_atom = (*env)->CallIntMethod(env, input, getAtomIndex, cat);
//        istereo->neighbor[0] = (*env)->CallIntMethod(env, input, getAtomIndex, nat0);
//        istereo->neighbor[1] = (*env)->CallIntMethod(env, input, getAtomIndex, nat1);
//        istereo->neighbor[2] = (*env)->CallIntMethod(env, input, getAtomIndex, nat2);
//        istereo->neighbor[3] = (*env)->CallIntMethod(env, input, getAtomIndex, nat3);
//
//        istereo->type = (*env)->CallIntMethod(env, stereo, getStereoType);
//        istereo->parity = (*env)->CallIntMethod(env, stereo, getParity);
//
//    }
//
//    joptions = (jstring) (*env)->CallObjectMethod(env, input, getOptions);
//    options = (*env)->GetStringUTFChars(env, joptions, &iscopy);
//    opts = malloc(sizeof(char) * (strlen(options)+1));
//    strcpy(opts, options);
//
//    (*inchi_input).szOptions = opts;
//    (*env)->ReleaseStringUTFChars(env, joptions, options);
//
//    (*inchi_input).num_atoms = natoms;
//    (*inchi_input).num_stereo0D = nstereo;
//
//    (*inchi_input).atom = atoms;
//    (*inchi_input).stereo0D = stereos;
//
//    return 1;
//
//}
//
//
//
//
//  protected native static String LibInchiGetVersion();
//
//  private native JniInchiOutput GetStdINCHI(JniInchiInput input);
//
//  private native JniInchiOutput GetINCHIfromINCHI(String inchi, String options);
//
//  private native JniInchiOutputStructure GetStructFromINCHI(String inchi, String options);
//
//  private native JniInchiOutputKey GetINCHIKeyFromINCHI(String inchi);
//
//  private native JniInchiOutputKey GetStdINCHIKeyFromStdINCHI(String inchi);
//
//  private native int CheckINCHIKey(String key);
//
//  private native int CheckINCHI(String inchi, boolean strict);
//
//  private native JniInchiInputData GetINCHIInputFromAuxInfo(String auxInfo, boolean bDoNotAddH, boolean bDiffUnkUndfStereo);
//
//
//  private native JniInchiOutput GetINCHI(inchi_Input inchi_input,
//                                         inchi_Output inchi_output);
//
//
//
//
}
