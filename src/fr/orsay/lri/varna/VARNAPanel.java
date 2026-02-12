/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2012  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Universit� Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.9.
 VARNA version 3.9 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.9 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */

/*
 GNU GENERAL PUBLIC LICENSE
 Version 3, 29 June 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

 Preamble

 The GNU General Public License is a free, copyleft license for
 software and other kinds of works.

 The licenses for most software and other practical works are designed
 to take away your freedom to share and change the works.  By contrast,
 the GNU General Public License is intended to guarantee your freedom to
 share and change all versions of a program--to make sure it remains free
 software for all its users.  We, the Free Software Foundation, use the
 GNU General Public License for most of our software; it applies also to
 any other work released this way by its authors.  You can apply it to
 your programs, too.

 When we speak of free software, we are referring to freedom, not
 price.  Our General Public Licenses are designed to make sure that you
 have the freedom to distribute copies of free software (and charge for
 them if you wish), that you receive source code or can get it if you
 want it, that you can change the software or use pieces of it in new
 free programs, and that you know you can do these things.

 To protect your rights, we need to prevent others from denying you
 these rights or asking you to surrender the rights.  Therefore, you have
 certain responsibilities if you distribute copies of the software, or if
 you modify it: responsibilities to respect the freedom of others.

 For example, if you distribute copies of such a program, whether
 gratis or for a fee, you must pass on to the recipients the same
 freedoms that you received.  You must make sure that they, too, receive
 or can get the source code.  And you must show them these terms so they
 know their rights.

 Developers that use the GNU GPL protect your rights with two steps:
 (1) assert copyright on the software, and (2) offer you this License
 giving you legal permission to copy, distribute and/or modify it.

 For the developers' and authors' protection, the GPL clearly explains
 that there is no warranty for this free software.  For both users' and
 authors' sake, the GPL requires that modified versions be marked as
 changed, so that their problems will not be attributed erroneously to
 authors of previous versions.

 Some devices are designed to deny users access to install or run
 modified versions of the software inside them, although the manufacturer
 can do so.  This is fundamentally incompatible with the aim of
 protecting users' freedom to change the software.  The systematic
 pattern of such abuse occurs in the area of products for individuals to
 use, which is precisely where it is most unacceptable.  Therefore, we
 have designed this version of the GPL to prohibit the practice for those
 products.  If such problems arise substantially in other domains, we
 stand ready to extend this provision to those domains in future versions
 of the GPL, as needed to protect the freedom of users.

 Finally, every program is threatened constantly by software patents.
 States should not allow patents to restrict development and use of
 software on general-purpose computers, but in those that do, we wish to
 avoid the special danger that patents applied to a free program could
 make it effectively proprietary.  To prevent this, the GPL assures that
 patents cannot be used to render the program non-free.

 The precise terms and conditions for copying, distribution and
 modification follow.

 TERMS AND CONDITIONS

 0. Definitions.

 "This License" refers to version 3 of the GNU General Public License.

 "Copyright" also means copyright-like laws that apply to other kinds of
 works, such as semiconductor masks.

 "The Program" refers to any copyrightable work licensed under this
 License.  Each licensee is addressed as "you".  "Licensees" and
 "recipients" may be individuals or organizations.

 To "modify" a work means to copy from or adapt all or part of the work
 in a fashion requiring copyright permission, other than the making of an
 exact copy.  The resulting work is called a "modified version" of the
 earlier work or a work "based on" the earlier work.

 A "covered work" means either the unmodified Program or a work based
 on the Program.

 To "propagate" a work means to do anything with it that, without
 permission, would make you directly or secondarily liable for
 infringement under applicable copyright law, except executing it on a
 computer or modifying a private copy.  Propagation includes copying,
 distribution (with or without modification), making available to the
 public, and in some countries other activities as well.

 To "convey" a work means any kind of propagation that enables other
 parties to make or receive copies.  Mere interaction with a user through
 a computer network, with no transfer of a copy, is not conveying.

 An interactive user interface displays "Appropriate Legal Notices"
 to the extent that it includes a convenient and prominently visible
 feature that (1) displays an appropriate copyright notice, and (2)
 tells the user that there is no warranty for the work (except to the
 extent that warranties are provided), that licensees may convey the
 work under this License, and how to view a copy of this License.  If
 the interface presents a list of user commands or options, such as a
 menu, a prominent item in the list meets this criterion.

 1. Source Code.

 The "source code" for a work means the preferred form of the work
 for making modifications to it.  "Object code" means any non-source
 form of a work.

 A "Standard Interface" means an interface that either is an official
 standard defined by a recognized standards body, or, in the case of
 interfaces specified for a particular programming language, one that
 is widely used among developers working in that language.

 The "System Libraries" of an executable work include anything, other
 than the work as a whole, that (a) is included in the normal form of
 packaging a Major Component, but which is not part of that Major
 Component, and (b) serves only to enable use of the work with that
 Major Component, or to implement a Standard Interface for which an
 implementation is available to the public in source code form.  A
 "Major Component", in this context, means a major essential component
 (kernel, window system, and so on) of the specific operating system
 (if any) on which the executable work runs, or a compiler used to
 produce the work, or an object code interpreter used to run it.

 The "Corresponding Source" for a work in object code form means all
 the source code needed to generate, install, and (for an executable
 work) run the object code and to modify the work, including scripts to
 control those activities.  However, it does not include the work's
 System Libraries, or general-purpose tools or generally available free
 programs which are used unmodified in performing those activities but
 which are not part of the work.  For example, Corresponding Source
 includes interface definition files associated with source files for
 the work, and the source code for shared libraries and dynamically
 linked subprograms that the work is specifically designed to require,
 such as by intimate data communication or control flow between those
 subprograms and other parts of the work.

 The Corresponding Source need not include anything that users
 can regenerate automatically from other parts of the Corresponding
 Source.

 The Corresponding Source for a work in source code form is that
 same work.

 2. Basic Permissions.

 All rights granted under this License are granted for the term of
 copyright on the Program, and are irrevocable provided the stated
 conditions are met.  This License explicitly affirms your unlimited
 permission to run the unmodified Program.  The output from running a
 covered work is covered by this License only if the output, given its
 content, constitutes a covered work.  This License acknowledges your
 rights of fair use or other equivalent, as provided by copyright law.

 You may make, run and propagate covered works that you do not
 convey, without conditions so long as your license otherwise remains
 in force.  You may convey covered works to others for the sole purpose
 of having them make modifications exclusively for you, or provide you
 with facilities for running those works, provided that you comply with
 the terms of this License in conveying all material for which you do
 not control copyright.  Those thus making or running the covered works
 for you must do so exclusively on your behalf, under your direction
 and control, on terms that prohibit them from making any copies of
 your copyrighted material outside their relationship with you.

 Conveying under any other circumstances is permitted solely under
 the conditions stated below.  Sublicensing is not allowed; section 10
 makes it unnecessary.

 3. Protecting Users' Legal Rights From Anti-Circumvention Law.

 No covered work shall be deemed part of an effective technological
 measure under any applicable law fulfilling obligations under article
 11 of the WIPO copyright treaty adopted on 20 December 1996, or
 similar laws prohibiting or restricting circumvention of such
 measures.

 When you convey a covered work, you waive any legal power to forbid
 circumvention of technological measures to the extent such circumvention
 is effected by exercising rights under this License with respect to
 the covered work, and you disclaim any intention to limit operation or
 modification of the work as a means of enforcing, against the work's
 users, your or third parties' legal rights to forbid circumvention of
 technological measures.

 4. Conveying Verbatim Copies.

 You may convey verbatim copies of the Program's source code as you
 receive it, in any medium, provided that you conspicuously and
 appropriately publish on each copy an appropriate copyright notice;
 keep intact all notices stating that this License and any
 non-permissive terms added in accord with section 7 apply to the code;
 keep intact all notices of the absence of any warranty; and give all
 recipients a copy of this License along with the Program.

 You may charge any price or no price for each copy that you convey,
 and you may offer support or warranty protection for a fee.

 5. Conveying Modified Source Versions.

 You may convey a work based on the Program, or the modifications to
 produce it from the Program, in the form of source code under the
 terms of section 4, provided that you also meet all of these conditions:

 a) The work must carry prominent notices stating that you modified
 it, and giving a relevant date.

 b) The work must carry prominent notices stating that it is
 released under this License and any conditions added under section
 7.  This requirement modifies the requirement in section 4 to
 "keep intact all notices".

 c) You must license the entire work, as a whole, under this
 License to anyone who comes into possession of a copy.  This
 License will therefore apply, along with any applicable section 7
 additional terms, to the whole of the work, and all its parts,
 regardless of how they are packaged.  This License gives no
 permission to license the work in any other way, but it does not
 invalidate such permission if you have separately received it.

 d) If the work has interactive user interfaces, each must display
 Appropriate Legal Notices; however, if the Program has interactive
 interfaces that do not display Appropriate Legal Notices, your
 work need not make them do so.

 A compilation of a covered work with other separate and independent
 works, which are not by their nature extensions of the covered work,
 and which are not combined with it such as to form a larger program,
 in or on a volume of a storage or distribution medium, is called an
 "aggregate" if the compilation and its resulting copyright are not
 used to limit the access or legal rights of the compilation's users
 beyond what the individual works permit.  Inclusion of a covered work
 in an aggregate does not cause this License to apply to the other
 parts of the aggregate.

 6. Conveying Non-Source Forms.

 You may convey a covered work in object code form under the terms
 of sections 4 and 5, provided that you also convey the
 machine-readable Corresponding Source under the terms of this License,
 in one of these ways:

 a) Convey the object code in, or embodied in, a physical product
 (including a physical distribution medium), accompanied by the
 Corresponding Source fixed on a durable physical medium
 customarily used for software interchange.

 b) Convey the object code in, or embodied in, a physical product
 (including a physical distribution medium), accompanied by a
 written offer, valid for at least three years and valid for as
 long as you offer spare parts or customer support for that product
 model, to give anyone who possesses the object code either (1) a
 copy of the Corresponding Source for all the software in the
 product that is covered by this License, on a durable physical
 medium customarily used for software interchange, for a price no
 more than your reasonable cost of physically performing this
 conveying of source, or (2) access to copy the
 Corresponding Source from a network server at no charge.

 c) Convey individual copies of the object code with a copy of the
 written offer to provide the Corresponding Source.  This
 alternative is allowed only occasionally and noncommercially, and
 only if you received the object code with such an offer, in accord
 with subsection 6b.

 d) Convey the object code by offering access from a designated
 place (gratis or for a charge), and offer equivalent access to the
 Corresponding Source in the same way through the same place at no
 further charge.  You need not require recipients to copy the
 Corresponding Source along with the object code.  If the place to
 copy the object code is a network server, the Corresponding Source
 may be on a different server (operated by you or a third party)
 that supports equivalent copying facilities, provided you maintain
 clear directions next to the object code saying where to find the
 Corresponding Source.  Regardless of what server hosts the
 Corresponding Source, you remain obligated to ensure that it is
 available for as long as needed to satisfy these requirements.

 e) Convey the object code using peer-to-peer transmission, provided
 you inform other peers where the object code and Corresponding
 Source of the work are being offered to the general public at no
 charge under subsection 6d.

 A separable portion of the object code, whose source code is excluded
 from the Corresponding Source as a System Library, need not be
 included in conveying the object code work.

 A "User Product" is either (1) a "consumer product", which means any
 tangible personal property which is normally used for personal, family,
 or household purposes, or (2) anything designed or sold for incorporation
 into a dwelling.  In determining whether a product is a consumer product,
 doubtful cases shall be resolved in favor of coverage.  For a particular
 product received by a particular user, "normally used" refers to a
 typical or common use of that class of product, regardless of the status
 of the particular user or of the way in which the particular user
 actually uses, or expects or is expected to use, the product.  A product
 is a consumer product regardless of whether the product has substantial
 commercial, industrial or non-consumer uses, unless such uses represent
 the only significant mode of use of the product.

 "Installation Information" for a User Product means any methods,
 procedures, authorization keys, or other information required to install
 and execute modified versions of a covered work in that User Product from
 a modified version of its Corresponding Source.  The information must
 suffice to ensure that the continued functioning of the modified object
 code is in no case prevented or interfered with solely because
 modification has been made.

 If you convey an object code work under this section in, or with, or
 specifically for use in, a User Product, and the conveying occurs as
 part of a transaction in which the right of possession and use of the
 User Product is transferred to the recipient in perpetuity or for a
 fixed term (regardless of how the transaction is characterized), the
 Corresponding Source conveyed under this section must be accompanied
 by the Installation Information.  But this requirement does not apply
 if neither you nor any third party retains the ability to install
 modified object code on the User Product (for example, the work has
 been installed in ROM).

 The requirement to provide Installation Information does not include a
 requirement to continue to provide support service, warranty, or updates
 for a work that has been modified or installed by the recipient, or for
 the User Product in which it has been modified or installed.  Access to a
 network may be denied when the modification itself materially and
 adversely affects the operation of the network or violates the rules and
 protocols for communication across the network.

 Corresponding Source conveyed, and Installation Information provided,
 in accord with this section must be in a format that is publicly
 documented (and with an implementation available to the public in
 source code form), and must require no special password or key for
 unpacking, reading or copying.

 7. Additional Terms.

 "Additional permissions" are terms that supplement the terms of this
 License by making exceptions from one or more of its conditions.
 Additional permissions that are applicable to the entire Program shall
 be treated as though they were included in this License, to the extent
 that they are valid under applicable law.  If additional permissions
 apply only to part of the Program, that part may be used separately
 under those permissions, but the entire Program remains governed by
 this License without regard to the additional permissions.

 When you convey a copy of a covered work, you may at your option
 remove any additional permissions from that copy, or from any part of
 it.  (Additional permissions may be written to require their own
 removal in certain cases when you modify the work.)  You may place
 additional permissions on material, added by you to a covered work,
 for which you have or can give appropriate copyright permission.

 Notwithstanding any other provision of this License, for material you
 add to a covered work, you may (if authorized by the copyright holders of
 that material) supplement the terms of this License with terms:

 a) Disclaiming warranty or limiting liability differently from the
 terms of sections 15 and 16 of this License; or

 b) Requiring preservation of specified reasonable legal notices or
 author attributions in that material or in the Appropriate Legal
 Notices displayed by works containing it; or

 c) Prohibiting misrepresentation of the origin of that material, or
 requiring that modified versions of such material be marked in
 reasonable ways as different from the original version; or

 d) Limiting the use for publicity purposes of names of licensors or
 authors of the material; or

 e) Declining to grant rights under trademark law for use of some
 trade names, trademarks, or service marks; or

 f) Requiring indemnification of licensors and authors of that
 material by anyone who conveys the material (or modified versions of
 it) with contractual assumptions of liability to the recipient, for
 any liability that these contractual assumptions directly impose on
 those licensors and authors.

 All other non-permissive additional terms are considered "further
 restrictions" within the meaning of section 10.  If the Program as you
 received it, or any part of it, contains a notice stating that it is
 governed by this License along with a term that is a further
 restriction, you may remove that term.  If a license document contains
 a further restriction but permits relicensing or conveying under this
 License, you may add to a covered work material governed by the terms
 of that license document, provided that the further restriction does
 not survive such relicensing or conveying.

 If you add terms to a covered work in accord with this section, you
 must place, in the relevant source files, a statement of the
 additional terms that apply to those files, or a notice indicating
 where to find the applicable terms.

 Additional terms, permissive or non-permissive, may be stated in the
 form of a separately written license, or stated as exceptions;
 the above requirements apply either way.

 8. Termination.

 You may not propagate or modify a covered work except as expressly
 provided under this License.  Any attempt otherwise to propagate or
 modify it is void, and will automatically terminate your rights under
 this License (including any patent licenses granted under the third
 paragraph of section 11).

 However, if you cease all violation of this License, then your
 license from a particular copyright holder is reinstated (a)
 provisionally, unless and until the copyright holder explicitly and
 finally terminates your license, and (b) permanently, if the copyright
 holder fails to notify you of the violation by some reasonable means
 prior to 60 days after the cessation.

 Moreover, your license from a particular copyright holder is
 reinstated permanently if the copyright holder notifies you of the
 violation by some reasonable means, this is the first time you have
 received notice of violation of this License (for any work) from that
 copyright holder, and you cure the violation prior to 30 days after
 your receipt of the notice.

 Termination of your rights under this section does not terminate the
 licenses of parties who have received copies or rights from you under
 this License.  If your rights have been terminated and not permanently
 reinstated, you do not qualify to receive new licenses for the same
 material under section 10.

 9. Acceptance Not Required for Having Copies.

 You are not required to accept this License in order to receive or
 run a copy of the Program.  Ancillary propagation of a covered work
 occurring solely as a consequence of using peer-to-peer transmission
 to receive a copy likewise does not require acceptance.  However,
 nothing other than this License grants you permission to propagate or
 modify any covered work.  These actions infringe copyright if you do
 not accept this License.  Therefore, by modifying or propagating a
 covered work, you indicate your acceptance of this License to do so.

 10. Automatic Licensing of Downstream Recipients.

 Each time you convey a covered work, the recipient automatically
 receives a license from the original licensors, to run, modify and
 propagate that work, subject to this License.  You are not responsible
 for enforcing compliance by third parties with this License.

 An "entity transaction" is a transaction transferring control of an
 organization, or substantially all assets of one, or subdividing an
 organization, or merging organizations.  If propagation of a covered
 work results from an entity transaction, each party to that
 transaction who receives a copy of the work also receives whatever
 licenses to the work the party's predecessor in interest had or could
 give under the previous paragraph, plus a right to possession of the
 Corresponding Source of the work from the predecessor in interest, if
 the predecessor has it or can get it with reasonable efforts.

 You may not impose any further restrictions on the exercise of the
 rights granted or affirmed under this License.  For example, you may
 not impose a license fee, royalty, or other charge for exercise of
 rights granted under this License, and you may not initiate litigation
 (including a cross-claim or counterclaim in a lawsuit) alleging that
 any patent claim is infringed by making, using, selling, offering for
 sale, or importing the Program or any portion of it.

 11. Patents.

 A "contributor" is a copyright holder who authorizes use under this
 License of the Program or a work on which the Program is based.  The
 work thus licensed is called the contributor's "contributor version".

 A contributor's "essential patent claims" are all patent claims
 owned or controlled by the contributor, whether already acquired or
 hereafter acquired, that would be infringed by some manner, permitted
 by this License, of making, using, or selling its contributor version,
 but do not include claims that would be infringed only as a
 consequence of further modification of the contributor version.  For
 purposes of this definition, "control" includes the right to grant
 patent sublicenses in a manner consistent with the requirements of
 this License.

 Each contributor grants you a non-exclusive, worldwide, royalty-free
 patent license under the contributor's essential patent claims, to
 make, use, sell, offer for sale, import and otherwise run, modify and
 propagate the contents of its contributor version.

 In the following three paragraphs, a "patent license" is any express
 agreement or commitment, however denominated, not to enforce a patent
 (such as an express permission to practice a patent or covenant not to
 sue for patent infringement).  To "grant" such a patent license to a
 party means to make such an agreement or commitment not to enforce a
 patent against the party.

 If you convey a covered work, knowingly relying on a patent license,
 and the Corresponding Source of the work is not available for anyone
 to copy, free of charge and under the terms of this License, through a
 publicly available network server or other readily accessible means,
 then you must either (1) cause the Corresponding Source to be so
 available, or (2) arrange to deprive yourself of the benefit of the
 patent license for this particular work, or (3) arrange, in a manner
 consistent with the requirements of this License, to extend the patent
 license to downstream recipients.  "Knowingly relying" means you have
 actual knowledge that, but for the patent license, your conveying the
 covered work in a country, or your recipient's use of the covered work
 in a country, would infringe one or more identifiable patents in that
 country that you have reason to believe are valid.

 If, pursuant to or in connection with a single transaction or
 arrangement, you convey, or propagate by procuring conveyance of, a
 covered work, and grant a patent license to some of the parties
 receiving the covered work authorizing them to use, propagate, modify
 or convey a specific copy of the covered work, then the patent license
 you grant is automatically extended to all recipients of the covered
 work and works based on it.

 A patent license is "discriminatory" if it does not include within
 the scope of its coverage, prohibits the exercise of, or is
 conditioned on the non-exercise of one or more of the rights that are
 specifically granted under this License.  You may not convey a covered
 work if you are a party to an arrangement with a third party that is
 in the business of distributing software, under which you make payment
 to the third party based on the extent of your activity of conveying
 the work, and under which the third party grants, to any of the
 parties who would receive the covered work from you, a discriminatory
 patent license (a) in connection with copies of the covered work
 conveyed by you (or copies made from those copies), or (b) primarily
 for and in connection with specific products or compilations that
 contain the covered work, unless you entered into that arrangement,
 or that patent license was granted, prior to 28 March 2007.

 Nothing in this License shall be construed as excluding or limiting
 any implied license or other defenses to infringement that may
 otherwise be available to you under applicable patent law.

 12. No Surrender of Others' Freedom.

 If conditions are imposed on you (whether by court order, agreement or
 otherwise) that contradict the conditions of this License, they do not
 excuse you from the conditions of this License.  If you cannot convey a
 covered work so as to satisfy simultaneously your obligations under this
 License and any other pertinent obligations, then as a consequence you may
 not convey it at all.  For example, if you agree to terms that obligate you
 to collect a royalty for further conveying from those to whom you convey
 the Program, the only way you could satisfy both those terms and this
 License would be to refrain entirely from conveying the Program.

 13. Use with the GNU Affero General Public License.

 Notwithstanding any other provision of this License, you have
 permission to link or combine any covered work with a work licensed
 under version 3 of the GNU Affero General Public License into a single
 combined work, and to convey the resulting work.  The terms of this
 License will continue to apply to the part which is the covered work,
 but the special requirements of the GNU Affero General Public License,
 section 13, concerning interaction through a network will apply to the
 combination as such.

 14. Revised Versions of this License.

 The Free Software Foundation may publish revised and/or new versions of
 the GNU General Public License from time to time.  Such new versions will
 be similar in spirit to the present version, but may differ in detail to
 address new problems or concerns.

 Each version is given a distinguishing version number.  If the
 Program specifies that a certain numbered version of the GNU General
 Public License "or any later version" applies to it, you have the
 option of following the terms and conditions either of that numbered
 version or of any later version published by the Free Software
 Foundation.  If the Program does not specify a version number of the
 GNU General Public License, you may choose any version ever published
 by the Free Software Foundation.

 If the Program specifies that a proxy can decide which future
 versions of the GNU General Public License can be used, that proxy's
 public statement of acceptance of a version permanently authorizes you
 to choose that version for the Program.

 Later license versions may give you additional or different
 permissions.  However, no additional obligations are imposed on any
 author or copyright holder as a result of your choosing to follow a
 later version.

 15. Disclaimer of Warranty.

 THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY
 APPLICABLE LAW.  EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT
 HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY
 OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM
 IS WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
 ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

 16. Limitation of Liability.

 IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING
 WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS
 THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY
 GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE
 USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF
 DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD
 PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS),
 EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGES.

 17. Interpretation of Sections 15 and 16.

 If the disclaimer of warranty and limitation of liability provided
 above cannot be given local legal effect according to their terms,
 reviewing courts shall apply local law that most closely approximates
 an absolute waiver of all civil liability in connection with the
 Program, unless a warranty or assumption of liability accompanies a
 copy of the Program in return for a fee.

 END OF TERMS AND CONDITIONS
 */

package fr.orsay.lri.varna;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.undo.UndoManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.controlers.ControleurBlinkingThread;
import fr.orsay.lri.varna.controlers.ControleurClicMovement;
import fr.orsay.lri.varna.controlers.ControleurDraggedMolette;
import fr.orsay.lri.varna.controlers.ControleurInterpolator;
import fr.orsay.lri.varna.controlers.ControleurMolette;
import fr.orsay.lri.varna.controlers.ControleurVARNAPanelKeys;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceVARNABasesListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNARNAListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNASelectionListener;
import fr.orsay.lri.varna.models.BaseList;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.export.SwingGraphics;
import fr.orsay.lri.varna.models.export.VueVARNAGraphics;
import fr.orsay.lri.varna.models.rna.Mapping;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBackbone;
import fr.orsay.lri.varna.models.rna.ModeleBackboneElement.BackboneType;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleBasesComparison;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.utils.VARNASessionParser;
import fr.orsay.lri.varna.views.VueMenu;
import fr.orsay.lri.varna.views.VueUI;

/**
 * 
 * BH j2s SwingJS Added PropertyChangeListener for returns from VueUI.  
 * 
 *  
 *  
 * 
 * The RNA 2D Panel is a lightweight component that allows for an automatic
 * basic drawing of an RNA secondary structures. The drawing algorithms do not
 * ensure a non-overlapping drawing of helices, thus it is possible to "spin the
 * helices" through a click-and-drag approach. A typical usage of the class from
 * within the constructor of a <code>JFrame</code> would be the following:<br/>
 * <code>
 * &nbsp;&nbsp;VARNAPanel _rna = new VARNAPanel("CCCCAUAUGGGGACC","((((....))))...");<br />
 * &nbsp;&nbsp;this.getContentPane().add(_rna);
 * </code>
 * 
 * @version 3.4
 * @author Yann Ponty & Kevin Darty
 * 
 */

public class VARNAPanel extends JPanel implements PropertyChangeListener {
	
	/**
	 * SwingJS uses a PropertyChangeEvent to signal that a pseudo-modal dialog has been closed.
	 *   
	 * @param event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		Object val = event.getNewValue();
		switch (event.getPropertyName()) {
		case "value":
			_UI.onDialogReturn(val == null ? JOptionPane.CLOSED_OPTION : ((Integer) val).intValue());
			return;
		case "SelectedFile":
		case "SelectedColor":
		case "inputValue":
			_UI.onDialogReturn(val);
			break;
		}
	}

 
	
	private static final long serialVersionUID = 8194421570308956001L;

	private RNA _RNA = new RNA();

	private boolean _debug = false;

	private VARNAConfig _conf = new VARNAConfig();

	private ArrayList<InterfaceVARNAListener> _VARNAListeners = new ArrayList<InterfaceVARNAListener>();
	private ArrayList<InterfaceVARNASelectionListener> _selectionListeners = new ArrayList<InterfaceVARNASelectionListener>();
	private ArrayList<InterfaceVARNARNAListener> _RNAListeners = new ArrayList<InterfaceVARNARNAListener>();
	private ArrayList<InterfaceVARNABasesListener> _basesListeners = new ArrayList<InterfaceVARNABasesListener>();

	UndoManager _manager;

	// private boolean _foldMode = true;

	private Point2D.Double[] _realCoords = new Point2D.Double[0];
	private Point2D.Double[] _realCenters = new Point2D.Double[0];
	private double _scaleFactor = 1.0;
	private Point2D.Double _offsetPanel = new Point2D.Double();
	private Point2D.Double _offsetRNA = new Point2D.Double();

	private double _offX;
	private double _offY;

	private ControleurBlinkingThread _blink;
	private BaseList _selectedBases = new BaseList("selection");
	private ArrayList<ModeleBase> _backupSelection = new ArrayList<ModeleBase>();
	private Integer _nearestBase = null;
	private Point2D.Double _lastSelectedCoord = new Point2D.Double(0.0, 0.0);

	private Point2D.Double _linkOrigin = null;
	private Point2D.Double _linkDestination = null;

	private Rectangle _selectionRectangle = null;

	private boolean _highlightAnnotation = false;

	private int _titleHeight;
	private Dimension _border = new Dimension(0, 0);

	private boolean _drawBBox = false;
	private boolean _drawBorder = false;

	// private Point _positionRelativeSouris;
	private Point _translation;
	private boolean _horsCadre;
	private boolean _premierAffichage;

	private ControleurInterpolator _interpolator;
	/**
	 * If comparison mode is TRUE (ON), then the application will be used to
	 * display a super-structure resulting on an RNA secondary structure
	 * comparison. Else, the application is used by default.
	 */

	private VueMenu _popup = new VueMenu(this);

	private VueUI _UI = new VueUI(this);

	private TextAnnotation _selectedAnnotation;

	/**
	 * Creates an RNA 2D panel with initially displays the empty structure.
	 * 
	 * @throws ExceptionNonEqualLength
	 * 
	 */
	public VARNAPanel() {
		init();
		drawRNA();
	}

	/**
	 * Creates an RNA 2D panel, and creates and displays an RNA coupled with its
	 * secondary structure formatted as a well-balanced parenthesis with dots
	 * word (DBN format).
	 * 
	 * @param seq
	 *            The raw nucleotide sequence
	 * @param str
	 *            The secondary structure in DBN format
	 * @throws ExceptionNonEqualLength
	 */

	public VARNAPanel(String seq, String str) throws ExceptionNonEqualLength {
		this(seq, str, RNA.DRAW_MODE_RADIATE);
	}

	/**
	 * Creates a VARNAPanel instance, and creates and displays an RNA coupled
	 * with its secondary structure formatted as a well-balanced parenthesis
	 * with dots word (DBN format). Allows the user to choose the drawing
	 * algorithm to be used.
	 * 
	 * @param seq
	 *            The raw nucleotide sequence
	 * @param str
	 *            The secondary structure in DBN format
	 * @param drawMode
	 *            The drawing mode
	 * @throws ExceptionNonEqualLength
	 * @see RNA#DRAW_MODE_RADIATE
	 * @see RNA#DRAW_MODE_CIRCULAR
	 * @see RNA#DRAW_MODE_NAVIEW
	 */
	public VARNAPanel(String seq, String str, int drawMode)
			throws ExceptionNonEqualLength {
		this(seq, str, drawMode, "");
	}

	public VARNAPanel(Reader r) throws ExceptionNonEqualLength,
			ExceptionFileFormatOrSyntax {
		this(r, RNA.DRAW_MODE_RADIATE);
	}

	public VARNAPanel(Reader r, int drawMode) throws ExceptionNonEqualLength,
			ExceptionFileFormatOrSyntax {
		this(r, drawMode, "");
	}

	public VARNAPanel(Reader r, int drawMode, String title)
			throws ExceptionNonEqualLength, ExceptionFileFormatOrSyntax {
		init();
		drawRNA(r, drawMode);
		setTitle(title);
	}

	public void setOriginLink(Point2D.Double p) {
		_linkOrigin = (p);
	}

	public void setDestinationLink(Point2D.Double p) {
		_linkDestination = (p);
	}

	public void removeLink() {
		_linkOrigin = null;
		_linkDestination = null;
	}

	/**
	 * Creates a VARNAPanel instance, and displays an RNA.
	 * 
	 * @param r
	 *            The RNA to be displayed within this panel
	 */

	public VARNAPanel(RNA r) {
		showRNA(r);
		init();
	}

	/**
	 * Creates a VARNAPanel instance, and creates and displays an RNA coupled
	 * with its secondary structure formatted as a well-balanced parenthesis
	 * with dots word (DBN format). Allows the user to choose the drawing
	 * algorithm to be used. Additionally, sets the panel's title.
	 * 
	 * @param seq
	 *            The raw nucleotide sequence
	 * @param str
	 *            The secondary structure in DBN format
	 * @param drawMode
	 *            The drawing mode
	 * @param title
	 *            The panel title
	 * @throws ExceptionNonEqualLength
	 * @see RNA#DRAW_MODE_CIRCULAR
	 * @see RNA#DRAW_MODE_RADIATE
	 * @see RNA#DRAW_MODE_NAVIEW
	 */

	public VARNAPanel(String seq, String str, int drawMode, String title)
			throws ExceptionNonEqualLength {
		drawRNA(seq, str, drawMode);
		init();
		setTitle(title);
		// VARNASecDraw._vp = this;
	}

	public VARNAPanel(String seq1, String struct1, String seq2, String struct2,
			int drawMode, String title) {
		_conf._comparisonMode = true;
		drawRNA(seq1, struct1, seq2, struct2, drawMode);
		init();
		setTitle(title);
	}

	private void init() {
		setBackground(VARNAConfig.DEFAULT_BACKGROUND_COLOR);
		_manager = new UndoManager();
		_manager.setLimit(10000);
		_UI.addUndoableEditListener(_manager);

		_blink = new ControleurBlinkingThread(this,
				ControleurBlinkingThread.DEFAULT_FREQUENCY, 0, 1.0, 0.0, 0.2);
		_blink.start();

		_premierAffichage = true;
		_translation = new Point(0, 0);

		_horsCadre = false;
		this.setFont(_conf._fontBasesGeneral);

		// ajout des controleurs au VARNAPanel
		ControleurClicMovement controleurClicMovement = new ControleurClicMovement(
				this);
		this.addMouseListener(controleurClicMovement);
		this.addMouseMotionListener(controleurClicMovement);
		this.addMouseWheelListener(new ControleurMolette(this));

		ControleurDraggedMolette ctrlDraggedMolette = new ControleurDraggedMolette(
				this);
		this.addMouseMotionListener(ctrlDraggedMolette);
		this.addMouseListener(ctrlDraggedMolette);

		ControleurVARNAPanelKeys ctrlKey = new ControleurVARNAPanelKeys(this);
		this.addKeyListener(ctrlKey);
		this.addFocusListener(ctrlKey);

		_interpolator = new ControleurInterpolator(this);
		/**
		 * 
		 * BH SwingJS do not start this thread
		 * 
		 * @j2sNative 
		 */
		{
		_interpolator.start();
		}

	}

	public void undo() {
		if (_manager.canUndo())
			_manager.undo();
	}

	public void redo() {
		if (_manager.canRedo())
			_manager.redo();
	}

	/**
	 * Sets the new style of the title font.
	 * 
	 * @param newStyle
	 *            An int that describes the new font style ("PLAIN","BOLD",
	 *            "BOLDITALIC", or "ITALIC")
	 */
	public void setTitleFontStyle(int newStyle) {
		_conf._titleFont = _conf._titleFont.deriveFont(newStyle);
		updateTitleHeight();
	}

	/**
	 * Sets the new size of the title font.
	 * 
	 * @param newSize
	 *            The new size of the title font
	 */
	public void setTitleFontSize(float newSize) {
		//System.err.println("Applying title size "+newSize);
		_conf._titleFont = _conf._titleFont.deriveFont(newSize);
		updateTitleHeight();
	}

	/**
	 * Sets the new font family to be used for the title. Available fonts are
	 * system-specific, yet it seems that "Arial", "Dialog", and "MonoSpaced"
	 * are almost always available.
	 * 
	 * @param newFamily
	 *            New font family used for the title
	 */
	public void setTitleFontFamily(String newFamily) {
		_conf._titleFont = new Font(newFamily, _conf._titleFont.getStyle(),
				_conf._titleFont.getSize());
		updateTitleHeight();
	}

	/**
	 * Sets the color to be used for the title.
	 * 
	 * @param newColor
	 *            A color used to draw the title
	 */
	public void setTitleFontColor(Color newColor) {
		_conf._titleColor = newColor;
		updateTitleHeight();
	}

	/**
	 * Sets the font size for displaying bases
	 * 
	 * @param size
	 *            Font size for base caption
	 */

	public void setBaseFontSize(Float size) {
		_conf._fontBasesGeneral = _conf._fontBasesGeneral.deriveFont(size);
	}

	/**
	 * Sets the font size for displaying base numbers
	 * 
	 * @param size
	 *            Font size for base numbers
	 */

	public void setNumbersFontSize(Float size) {
		_conf._numbersFont = _conf._numbersFont.deriveFont(size);
	}

	/**
	 * Sets the font style for displaying bases
	 * 
	 * @param style
	 *            An int that describes the new font style ("PLAIN","BOLD",
	 *            "BOLDITALIC", or "ITALIC")
	 */

	public void setBaseFontStyle(int style) {
		_conf._fontBasesGeneral = _conf._fontBasesGeneral.deriveFont(style);
	}

	private void updateTitleHeight() {
		if (!getTitle().equals("")) {
			_titleHeight = (int) (_conf._titleFont.getSize() * 1.5);
		} else {
			_titleHeight = 0;
		}
		if (Math.abs(this.getZoom() - 1) < .02) {
			_translation.y = (int) (-getTitleHeight() / 2.0);
		}
	}

	/**
	 * Sets the panel's title, giving a short description of the RNA secondary
	 * structure.
	 * 
	 * @param title
	 *            The new title
	 */
	public void setTitle(String title) {
		_RNA.setName(title);
		updateTitleHeight();
	}

	/**
	 * Sets the distance between consecutive base numbers. Please notice that :
	 * <ul>
	 * <li>The first and last base are always numbered</li>
	 * <li>The numbering is based on the base numbers, not on the indices. So
	 * base numbers may appear more frequently than expected if bases are
	 * skipped</li>
	 * <li>The periodicity is measured starting from 0. This means that for a
	 * period of 10 and bases numbered from 1 to 52, the base numbers
	 * [1,10,20,30,40,50,52] will be drawn.</li>
	 * </ul>
	 * 
	 * @param n
	 *            New numbering period
	 */
	public void setNumPeriod(int n) {
		_conf._numPeriod = n;
	}

	/**
	 * Returns the current numbering period. Please notice that :
	 * <ul>
	 * <li>The first and last base are always numbered</li>
	 * <li>The numbering is based on the base numbers, not on the indices. So
	 * base numbers may appear more frequently than expected if bases are
	 * skipped</li>
	 * <li>The periodicity is measured starting from 0. This means that for a
	 * period of 10 and bases numbered from 1 to 52, the base numbers
	 * [1,10,20,30,40,50,52] will be drawn.</li>
	 * </ul>
	 * 
	 * @return Current numbering period
	 */
	public int getNumPeriod() {
		return _conf._numPeriod;
	}

	private void setScaleFactor(double d) {
		_scaleFactor = d;
	}

	private double getScaleFactor() {
		return _scaleFactor;
	}

	private void setAutoFit(boolean fit) {
		_conf._autoFit = fit;
		repaint();
	}

	public void lockScrolling() {
		setAutoFit(false);
		setAutoCenter(false);
	}

	public void unlockScrolling() {
		setAutoFit(true);
		setAutoCenter(true);
	}

	private void drawStringOutline(VueVARNAGraphics g2D, String res, double x,
			double y, double margin) {
		Dimension d = g2D.getStringDimension(res);
		x -= (double) d.width / 2.0;
		y += (double) d.height / 2.0;
		g2D.setColor(Color.GRAY);
		g2D.setSelectionStroke();
		g2D.drawRect((x - margin), (y - d.height - margin),
				(d.width + 2.0 * margin), (d.height + 2.0 * margin));
	}

	private void drawSymbol(VueVARNAGraphics g2D, double posx, double posy,
			double normx, double normy, double radius, boolean isCIS,
			ModeleBP.Edge e) {
		Color bck = g2D.getColor();
		switch (e) {
		case WC:
			if (isCIS) {
				g2D.setColor(bck);
				g2D.fillCircle((posx - (radius) / 2.0),
						(posy - (radius) / 2.0), radius);
				g2D.drawCircle((posx - (radius) / 2.0),
						(posy - (radius) / 2.0), radius);
			} else {
				g2D.setColor(Color.white);
				g2D.fillCircle(posx - (radius) / 2.0, (posy - (radius) / 2.0),
						(radius));
				g2D.setColor(bck);
				g2D.drawCircle((posx - (radius) / 2.0),
						(posy - (radius) / 2.0), (radius));
			}
			break;
		case HOOGSTEEN: {
			GeneralPath p2 = new GeneralPath();
			radius /= 1.05;
			p2.moveTo((float) (posx - radius * normx / 2.0 - radius * normy
					/ 2.0), (float) (posy - radius * normy / 2.0 + radius
					* normx / 2.0));
			p2.lineTo((float) (posx + radius * normx / 2.0 - radius * normy
					/ 2.0), (float) (posy + radius * normy / 2.0 + radius
					* normx / 2.0));
			p2.lineTo((float) (posx + radius * normx / 2.0 + radius * normy
					/ 2.0), (float) (posy + radius * normy / 2.0 - radius
					* normx / 2.0));
			p2.lineTo((float) (posx - radius * normx / 2.0 + radius * normy
					/ 2.0), (float) (posy - radius * normy / 2.0 - radius
					* normx / 2.0));
			p2.closePath();

			if (isCIS) {
				g2D.setColor(bck);
				g2D.fill(p2);
				g2D.draw(p2);
			} else {
				g2D.setColor(Color.white);
				g2D.fill(p2);
				g2D.setColor(bck);
				g2D.draw(p2);
			}
		}
			break;
		case SUGAR: {
			double ix = radius * normx / 2.0;
			double iy = radius * normy / 2.0;
			double jx = radius * normy / 2.0;
			double jy = -radius * normx / 2.0;

			GeneralPath p2 = new GeneralPath();
			p2.moveTo((float) (posx - ix + jx), (float) (posy - iy + jy));
			p2.lineTo((float) (posx + ix + jx), (float) (posy + iy + jy));
			p2.lineTo((float) (posx - jx), (float) (posy - jy));
			p2.closePath();

			if (isCIS) {
				g2D.setColor(bck);
				g2D.fill(p2);
				g2D.draw(p2);
			} else {
				g2D.setColor(Color.white);
				g2D.fill(p2);
				g2D.setColor(bck);
				g2D.draw(p2);
			}
		}
			break;
		}
		g2D.setColor(bck);
	}

	private void drawBasePairArc(VueVARNAGraphics g2D, int i, int j,
			Point2D.Double orig, Point2D.Double dest, double scaleFactor,
			ModeleBP style, double newRadius) {
		double distance, coef;
		if (j - i == 1)
			coef = getBPHeightIncrement() * 1.75;
		else
			coef = getBPHeightIncrement();
		distance = dest.x - orig.x;
		switch (_conf._mainBPStyle) {
		case LW: {
			double radiusCircle = ((RNA.BASE_PAIR_DISTANCE - _RNA.BASE_RADIUS) / 5.0)
					* scaleFactor;
			if (style.isCanonical()) {
				if (style.isCanonicalGC()) {
					if ((orig.x != dest.x) || (orig.y != dest.y)) {
						g2D.drawArc((dest.x + orig.x) / 2., dest.y
								- scaleFactor * _RNA.BASE_RADIUS / 2.0,
								(distance - scaleFactor * _RNA.BASE_RADIUS
										/ 3.0), (distance * coef - scaleFactor
										* _RNA.BASE_RADIUS / 3.0), 0, 180);
						g2D.drawArc((dest.x + orig.x) / 2., dest.y
								- scaleFactor * _RNA.BASE_RADIUS / 2.0,
								(distance + scaleFactor * _RNA.BASE_RADIUS
										/ 3.0), (distance * coef + scaleFactor
										* _RNA.BASE_RADIUS / 3.0), 0, 180);
					}
				} else if (style.isCanonicalAU()) {
					g2D.drawArc((dest.x + orig.x) / 2., dest.y - scaleFactor
							* _RNA.BASE_RADIUS / 2.0, (distance),
							(distance * coef), 0, 180);
				} else if (style.isWobbleUG()) {
					Point2D.Double midtop = new Point2D.Double(
							(dest.x + orig.x) / 2., dest.y - distance * coef
									/ 2. - scaleFactor * _RNA.BASE_RADIUS / 2.0);
					g2D.drawArc(midtop.x, dest.y - scaleFactor
							* _RNA.BASE_RADIUS / 2.0, (distance),
							(distance * coef), 0, 180);
					drawSymbol(g2D, midtop.x, midtop.y, 1., 0., radiusCircle,
							false, ModeleBP.Edge.WC);
				} else {
					Point2D.Double midtop = new Point2D.Double(
							(dest.x + orig.x) / 2., dest.y - distance * coef
									/ 2. - scaleFactor * _RNA.BASE_RADIUS / 2.0);
					g2D.drawArc(midtop.x, dest.y - scaleFactor
							* _RNA.BASE_RADIUS / 2.0, (distance),
							(distance * coef), 0, 180);
					drawSymbol(g2D, midtop.x, midtop.y, 1., 0., radiusCircle,
							style.isCIS(), style.getEdgePartner5());
				}
			} else {
				ModeleBP.Edge p1 = style.getEdgePartner5();
				ModeleBP.Edge p2 = style.getEdgePartner3();
				Point2D.Double midtop = new Point2D.Double(
						(dest.x + orig.x) / 2., dest.y - distance * coef / 2.
								- scaleFactor * _RNA.BASE_RADIUS / 2.0);
				g2D.drawArc(midtop.x, dest.y - scaleFactor * _RNA.BASE_RADIUS
						/ 2.0, (distance), (distance * coef), 0, 180);
				if (p1 == p2) {
					drawSymbol(g2D, midtop.x, midtop.y, 1., 0., radiusCircle,
							false, style.getEdgePartner5());
				} else {
					drawSymbol(g2D, midtop.x - scaleFactor * _RNA.BASE_RADIUS,
							midtop.y, 1., 0., radiusCircle, style.isCIS(), p1);
					drawSymbol(g2D, midtop.x + scaleFactor * _RNA.BASE_RADIUS,
							midtop.y, -1., 0., radiusCircle, style.isCIS(), p2);
				}
			}
		}
			break;
		case LW_ALT: {
			double radiusCircle = ((RNA.BASE_PAIR_DISTANCE - _RNA.BASE_RADIUS) / 5.0)
					* scaleFactor;
			double distFromBaseCenter = DISTANCE_FACT*scaleFactor;
			orig = new Point2D.Double(orig.x,orig.y-(distFromBaseCenter+newRadius));
			dest = new Point2D.Double(dest.x,dest.y-(distFromBaseCenter+newRadius));
			if (style.isCanonical()) {
				if (style.isCanonicalGC()) {
					if ((orig.x != dest.x) || (orig.y != dest.y)) {
						g2D.drawArc((dest.x + orig.x) / 2., dest.y
								- scaleFactor * _RNA.BASE_RADIUS / 2.0,
								(distance - scaleFactor * _RNA.BASE_RADIUS
										/ 3.0), (distance * coef - scaleFactor
										* _RNA.BASE_RADIUS / 3.0), 0, 180);
						g2D.drawArc((dest.x + orig.x) / 2., dest.y
								- scaleFactor * _RNA.BASE_RADIUS / 2.0,
								(distance + scaleFactor * _RNA.BASE_RADIUS
										/ 3.0), (distance * coef + scaleFactor
										* _RNA.BASE_RADIUS / 3.0), 0, 180);
					}
				} else if (style.isCanonicalAU()) {
					g2D.drawArc((dest.x + orig.x) / 2., dest.y - scaleFactor
							* _RNA.BASE_RADIUS / 2.0, (distance),
							(distance * coef), 0, 180);
				}
			} else {
				ModeleBP.Edge p1 = style.getEdgePartner5();
				ModeleBP.Edge p2 = style.getEdgePartner3();
				Point2D.Double midtop = new Point2D.Double(
						(dest.x + orig.x) / 2., dest.y - distance * coef / 2.
								- scaleFactor * _RNA.BASE_RADIUS / 2.0);
				g2D.drawArc(midtop.x, dest.y - scaleFactor * _RNA.BASE_RADIUS
						/ 2.0, (distance), (distance * coef), 0, 180);
				drawSymbol(g2D, orig.x,
							orig.y-radiusCircle*.95, 1., 0., radiusCircle, style.isCIS(), p1);
				drawSymbol(g2D, dest.x,
							dest.y-radiusCircle*.95, -1., 0., radiusCircle, style.isCIS(), p2);
			}
		}
			break;
		default:
			g2D.drawArc((dest.x + orig.x) / 2., dest.y - scaleFactor
					* _RNA.BASE_RADIUS / 2.0, (distance), (distance * coef), 0,
					180);
			break;
		}

	}

	public static double DISTANCE_FACT = 2.;

	
	private void drawBasePair(VueVARNAGraphics g2D, Point2D.Double orig,
			Point2D.Double dest, ModeleBP style, double newRadius,
			double scaleFactor) {

		double dx = dest.x - orig.x;
		double dy = dest.y - orig.y;
		double dist = Math.sqrt((dest.x - orig.x) * (dest.x - orig.x)
				+ (dest.y - orig.y) * (dest.y - orig.y));
		dx /= dist;
		dy /= dist;
		double nx = -dy;
		double ny = dx;
		orig = new Point2D.Double(orig.x + newRadius * dx, orig.y + newRadius
				* dy);
		dest = new Point2D.Double(dest.x - newRadius * dx, dest.y - newRadius
				* dy);
		switch (_conf._mainBPStyle) {
		case LW: {
			double radiusCircle = ((RNA.BASE_PAIR_DISTANCE - _RNA.BASE_RADIUS) / 5.0)
					* scaleFactor;
			if (style.isCanonical()) {
				if (style.isCanonicalGC()) {
					if ((orig.x != dest.x) || (orig.y != dest.y)) {
						nx *= scaleFactor * _RNA.BASE_RADIUS / 4.0;
						ny *= scaleFactor * _RNA.BASE_RADIUS / 4.0;
						g2D.drawLine((orig.x + nx), (orig.y + ny),
								(dest.x + nx), (dest.y + ny));
						g2D.drawLine((orig.x - nx), (orig.y - ny),
								(dest.x - nx), (dest.y - ny));
					}
				} else if (style.isCanonicalAU()) {
					g2D.drawLine(orig.x, orig.y, dest.x, dest.y);
				} else if (style.isWobbleUG()) {
					double cx = (dest.x + orig.x) / 2.0;
					double cy = (dest.y + orig.y) / 2.0;
					g2D.drawLine(orig.x, orig.y, dest.x, dest.y);
					drawSymbol(g2D, cx, cy, nx, ny, radiusCircle, false,
							ModeleBP.Edge.WC);
				} else {
					double cx = (dest.x + orig.x) / 2.0;
					double cy = (dest.y + orig.y) / 2.0;
					g2D.drawLine(orig.x, orig.y, dest.x, dest.y);
					drawSymbol(g2D, cx, cy, nx, ny, radiusCircle,
							style.isCIS(), style.getEdgePartner5());
				}
			} else {
				ModeleBP.Edge p1 = style.getEdgePartner5();
				ModeleBP.Edge p2 = style.getEdgePartner3();
				double cx = (dest.x + orig.x) / 2.0;
				double cy = (dest.y + orig.y) / 2.0;
				g2D.drawLine(orig.x, orig.y, dest.x, dest.y);
				if (p1 == p2) {
					drawSymbol(g2D, cx, cy, nx, ny, radiusCircle,
							style.isCIS(), p1);

				} else {
					double vdx = (dest.x - orig.x);
					double vdy = (dest.y - orig.y);
					vdx /= 6.0;
					vdy /= 6.0;
					drawSymbol(g2D, cx + vdx, cy + vdy, -nx, -ny, radiusCircle,
							style.isCIS(), p2);
					drawSymbol(g2D, cx - vdx, cy - vdy, nx, ny, radiusCircle,
							style.isCIS(), p1);
				}
			}
		}
			break;
		case LW_ALT: {
			double radiusCircle = ((RNA.BASE_PAIR_DISTANCE - _RNA.BASE_RADIUS) / 5.0)
					* scaleFactor;
			double distFromBaseCenter = DISTANCE_FACT*scaleFactor;
			Point2D.Double norig = new Point2D.Double(orig.x+(distFromBaseCenter+.5*newRadius)*dx,orig.y+(distFromBaseCenter+.5*newRadius)*dy);
			Point2D.Double ndest = new Point2D.Double(dest.x-(distFromBaseCenter+.5*newRadius)*dx,dest.y-(distFromBaseCenter+.5*newRadius)*dy);
			if (style.isCanonical()) {
				if (style.isCanonicalGC()) {
					if ((norig.x != ndest.x) || (norig.y != ndest.y)) {
						nx *= scaleFactor * _RNA.BASE_RADIUS / 4.0;
						ny *= scaleFactor * _RNA.BASE_RADIUS / 4.0;
						g2D.drawLine((norig.x + nx), (norig.y + ny),
								(ndest.x + nx), (ndest.y + ny));
						g2D.drawLine((norig.x - nx), (norig.y - ny),
								(ndest.x - nx), (ndest.y - ny));
					}
				} else if (style.isCanonicalAU()) {
					g2D.drawLine(norig.x, norig.y, ndest.x, ndest.y);
				} else if (style.isWobbleUG()) {
					double cx = (ndest.x + norig.x) / 2.0;
					double cy = (ndest.y + norig.y) / 2.0;
					g2D.drawLine(norig.x, norig.y, ndest.x, ndest.y);
					drawSymbol(g2D, cx, cy, nx, ny, radiusCircle, false,
							ModeleBP.Edge.WC);
				} else {
					double cx = (ndest.x + norig.x) / 2.0;
					double cy = (ndest.y + norig.y) / 2.0;
					g2D.drawLine(norig.x, norig.y, ndest.x, ndest.y);
					drawSymbol(g2D, cx, cy, nx, ny, radiusCircle,
							style.isCIS(), style.getEdgePartner5());
				}
			} else {
				ModeleBP.Edge p1 = style.getEdgePartner5();
				ModeleBP.Edge p2 = style.getEdgePartner3();
				double cx = (ndest.x + norig.x) / 2.0;
				double cy = (ndest.y + norig.y) / 2.0;
				g2D.drawLine(norig.x, norig.y, ndest.x, ndest.y);
				if (p1 == p2) {
					drawSymbol(g2D, cx, cy, nx, ny, radiusCircle,
							style.isCIS(), p1);

				} else {
					double fac = .4;
					drawSymbol(g2D, ndest.x - fac*radiusCircle*dx, ndest.y - fac*radiusCircle*dy, -nx, -ny, radiusCircle,
							style.isCIS(), p2);
					drawSymbol(g2D, norig.x + fac*radiusCircle*dx, norig.y + fac*radiusCircle*dy, nx, ny, radiusCircle,
							style.isCIS(), p1);
				}
			}
		}
			break;
		case SIMPLE:
			g2D.drawLine(orig.x, orig.y, dest.x, dest.y);
			break;
		case RNAVIZ:
			double xcenter = (orig.x + dest.x) / 2.0;
			double ycenter = (orig.y + dest.y) / 2.0;
			double radius = Math.max(4.0 * scaleFactor, 1.0);
			g2D.fillCircle((xcenter - radius), (ycenter - radius),
					(2.0 * radius));
			break;
		case NONE:
			break;
		}
	}

	private Color getHighlightedVersion(Color c1, Color c2) {
		int r1 = c1.getRed();
		int g1 = c1.getGreen();
		int b1 = c1.getBlue();
		int r2 = c2.getRed();
		int g2 = c2.getGreen();
		int b2 = c2.getBlue();
		double val = _blink.getVal();
		int nr = Math.max(0,
				Math.min((int) ((r1 * val + r2 * (1.0 - val))), 255));
		int ng = Math.max(0,
				Math.min((int) ((g1 * val + g2 * (1.0 - val))), 255));
		int nb = Math.max(0,
				Math.min((int) ((b1 * val + b2 * (1.0 - val))), 255));
		return new Color(nr, ng, nb);
	}

	private Color highlightFilter(int index, Color initialColor, Color c1,
			Color c2, boolean localView) {
		if (_selectedBases.contains(_RNA.getBaseAt(index)) && localView) {
			return getHighlightedVersion(c1, c2);
		} else
			return initialColor;
	}

	public static Point2D.Double computeExcentricUnitVector(int i,
			Point2D.Double[] points, Point2D.Double[] centers) {
		double dist = points[i].distance(centers[i]);
		Point2D.Double byCenter = new Point2D.Double(
				(points[i].x - centers[i].x) / dist,
				(points[i].y - centers[i].y) / dist);
		if ((i > 0) && (i < points.length - 1)) {
			Point2D.Double p0 = points[i - 1];
			Point2D.Double p1 = points[i];
			Point2D.Double p2 = points[i + 1];
			double dist1 = p2.distance(p1);
			Point2D.Double v1 = new Point2D.Double((p2.x - p1.x) / dist1,
					(p2.y - p1.y) / dist1);
			Point2D.Double vn1 = new Point2D.Double(v1.y, -v1.x);
			double dist2 = p1.distance(p0);
			Point2D.Double v2 = new Point2D.Double((p1.x - p0.x) / dist2,
					(p1.y - p0.y) / dist2);
			Point2D.Double vn2 = new Point2D.Double(v2.y, -v2.x);
			Point2D.Double vn = new Point2D.Double((vn1.x + vn2.x) / 2.0,
					(vn1.y + vn2.y) / 2.0);
			double D = vn.distance(new Point2D.Double(0.0, 0.0));
			vn.x /= D;
			vn.y /= D;
			if (byCenter.x * vn.x + byCenter.y * vn.y < 0) {
				vn.x = -vn.x;
				vn.y = -vn.y;
			}
			return vn;
		} 
		else if (((i==0) || (i==points.length-1)) && (points.length>1)) {
			int a = (i==0)?0:points.length-1;
			int b = (i==0)?1:points.length-2;
			double D = points[a].distance(points[b]);
			return new Point2D.Double(
					(points[a].x - points[b].x) / D,
					(points[a].y - points[b].y) / D);
		}
		else {
			return byCenter;
		}
	}

	private void drawBase(VueVARNAGraphics g2D, int i, Point2D.Double[] points,
			Point2D.Double[] centers, double newRadius, double _scaleFactor,
			boolean localView) {
		Point2D.Double p = points[i];
		ModeleBase mb = _RNA.get_listeBases().get(i);
		g2D.setFont(_conf._fontBasesGeneral);
		Color baseInnerColor = highlightFilter(i,
				_RNA.getBaseInnerColor(i, _conf), Color.white,
				_RNA.getBaseInnerColor(i, _conf), localView);
		Color baseOuterColor = highlightFilter(i,
				_RNA.getBaseOuterColor(i, _conf),
				_RNA.getBaseOuterColor(i, _conf), Color.white, localView);
		Color baseNameColor = highlightFilter(i,
				_RNA.getBaseNameColor(i, _conf),
				_RNA.getBaseNameColor(i, _conf), Color.white, localView);
		if ( RNA.whiteLabelPreferrable(baseInnerColor))
		{
			baseNameColor=Color.white;
		}

		if (mb instanceof ModeleBaseNucleotide) {
			ModeleBaseNucleotide mbn = (ModeleBaseNucleotide) mb;
			String res = mbn.getBase();
			if (_hoveredBase == mb && localView && isModifiable()) {
				g2D.setColor(_conf._hoverColor);
				g2D.fillCircle(p.getX() - 1.5 * newRadius, p.getY() - 1.5
						* newRadius, 3.0 * newRadius);
				g2D.setColor(_conf._hoverColor.darker());
				g2D.drawCircle(p.getX() - 1.5 * newRadius, p.getY() - 1.5
						* newRadius, 3.0 * newRadius);
				g2D.setPlainStroke();
			}
			if (_conf._fillBases) {
				// Filling inner circle
				g2D.setColor(baseInnerColor);
				g2D.fillCircle(p.getX() - newRadius, p.getY() - newRadius,
						2.0 * newRadius);
			}

			if (_conf._drawOutlineBases) {
				// Drawing outline
				g2D.setColor(baseOuterColor);
				g2D.setStrokeThickness(_conf._baseThickness * _scaleFactor);
				g2D.drawCircle(p.getX() - newRadius, p.getY() - newRadius,
						2.0 * newRadius);
			}
			// Drawing label
			g2D.setColor(baseNameColor);
			g2D.drawStringCentered(String.valueOf(res), p.getX(), p.getY());
		} else if (mb instanceof ModeleBasesComparison) {

			ModeleBasesComparison mbc = (ModeleBasesComparison) mb;

			// On lui donne l'aspect voulue (on a un trait droit)
			g2D.setPlainStroke(); // On doit avoir un trait droit, sans arrondit
			g2D.setStrokeThickness(_conf._baseThickness * _scaleFactor);

			// On dessine l'étiquette, rectangle aux bords arrondies.
			g2D.setColor(baseInnerColor);
			g2D.fillRoundRect((p.getX() - 1.5 * newRadius),
					(p.getY() - newRadius), (3.0 * newRadius),
					(2.0 * newRadius), 10 * _scaleFactor, 10 * _scaleFactor);

			/* Dessin du rectangle exterieur (bords) */
			g2D.setColor(baseOuterColor);
			g2D.drawRoundRect((p.getX() - 1.5 * newRadius),
					(p.getY() - newRadius), (3 * newRadius), (2 * newRadius),
					10 * _scaleFactor, 10 * _scaleFactor);

			// On le dessine au centre de l'étiquette.
			g2D.drawLine((p.getX()), (p.getY() + newRadius) - 1, (p.getX()),
					(p.getY() - newRadius) + 1);

			/* Dessin du nom de la base (A,C,G,U,etc...) */
			// On créer le texte des étiquettes
			String label1 = String.valueOf(mbc.getBase1());
			String label2 = String.valueOf(mbc.getBase2());

			// On leur donne une couleur
			g2D.setColor(getRNA().get_listeBases().get(i).getStyleBase()
					.getBaseNameColor());

			// Et on les dessine.
			g2D.drawStringCentered(label1, p.getX() - (.75 * newRadius),
					p.getY());
			g2D.drawStringCentered(label2, p.getX() + (.75 * newRadius),
					p.getY());
		}

		// Drawing base number
		if (_RNA.isNumberDrawn(mb, getNumPeriod())) {

			Point2D.Double vn = computeExcentricUnitVector(i, points, centers);
			g2D.setColor(mb.getStyleBase().getBaseNumberColor());
			g2D.setFont(_conf._numbersFont);
			double factorMin = Math.min(.5, _conf._distNumbers);
			double factorMax = Math.min(_conf._distNumbers - 1.5,
					_conf._distNumbers);
			g2D.drawLine(p.x + vn.x * ((1 + factorMin) * newRadius), p.y + vn.y
					* ((1 + factorMin) * newRadius), p.x + vn.x
					* ((1 + factorMax) * newRadius), p.y + vn.y
					* ((1 + factorMax) * newRadius));
			g2D.drawStringCentered(mb.getLabel(), p.x + vn.x
					* ((1 + _conf._distNumbers) * newRadius), p.y + vn.y
					* ((1 + _conf._distNumbers) * newRadius));

		}
	}

	void drawChemProbAnnotation(VueVARNAGraphics g2D, ChemProbAnnotation cpa,
			Point2D.Double anchor, double scaleFactor) {
		g2D.setColor(cpa.getColor());
		g2D.setStrokeThickness(RNA.CHEM_PROB_ARROW_THICKNESS * scaleFactor
				* cpa.getIntensity());
		g2D.setPlainStroke();
		Point2D.Double v = cpa.getDirVector();
		Point2D.Double vn = cpa.getNormalVector();
		Point2D.Double base = new Point2D.Double(
				(anchor.x + _RNA.CHEM_PROB_DIST * scaleFactor * v.x),
				(anchor.y + _RNA.CHEM_PROB_DIST * scaleFactor * v.y));
		Point2D.Double edge = new Point2D.Double(
				(base.x + _RNA.CHEM_PROB_BASE_LENGTH * cpa.getIntensity()
						* scaleFactor * v.x),
				(base.y + _RNA.CHEM_PROB_BASE_LENGTH * cpa.getIntensity()
						* scaleFactor * v.y));
		switch (cpa.getType()) {
		case ARROW: {
			Point2D.Double arrowTip1 = new Point2D.Double(
					(base.x + cpa.getIntensity()
							* scaleFactor
							* (_RNA.CHEM_PROB_ARROW_WIDTH * vn.x + _RNA.CHEM_PROB_ARROW_HEIGHT
									* v.x)),
					(base.y + cpa.getIntensity()
							* scaleFactor
							* (_RNA.CHEM_PROB_ARROW_WIDTH * vn.y + _RNA.CHEM_PROB_ARROW_HEIGHT
									* v.y)));
			Point2D.Double arrowTip2 = new Point2D.Double(
					(base.x + cpa.getIntensity()
							* scaleFactor
							* (-_RNA.CHEM_PROB_ARROW_WIDTH * vn.x + _RNA.CHEM_PROB_ARROW_HEIGHT
									* v.x)),
					(base.y + cpa.getIntensity()
							* scaleFactor
							* (-_RNA.CHEM_PROB_ARROW_WIDTH * vn.y + _RNA.CHEM_PROB_ARROW_HEIGHT
									* v.y)));
			g2D.drawLine(base.x, base.y, edge.x, edge.y);
			g2D.drawLine(base.x, base.y, arrowTip1.x, arrowTip1.y);
			g2D.drawLine(base.x, base.y, arrowTip2.x, arrowTip2.y);
		}
			break;
		case PIN: {
			Point2D.Double side1 = new Point2D.Double(
					(edge.x - cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * v.x)),
					(edge.y - cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * v.y)));
			Point2D.Double side2 = new Point2D.Double(
					(edge.x - cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * vn.x)),
					(edge.y - cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * vn.y)));
			Point2D.Double side3 = new Point2D.Double(
					(edge.x + cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * v.x)),
					(edge.y + cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * v.y)));
			Point2D.Double side4 = new Point2D.Double(
					(edge.x + cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * vn.x)),
					(edge.y + cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_PIN_SEMIDIAG * vn.y)));
			GeneralPath p2 = new GeneralPath();
			p2.moveTo((float) side1.x, (float) side1.y);
			p2.lineTo((float) side2.x, (float) side2.y);
			p2.lineTo((float) side3.x, (float) side3.y);
			p2.lineTo((float) side4.x, (float) side4.y);
			p2.closePath();
			g2D.fill(p2);
			g2D.drawLine(base.x, base.y, edge.x, edge.y);
		}
			break;
		case TRIANGLE: {
			Point2D.Double arrowTip1 = new Point2D.Double(
					(edge.x + cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_TRIANGLE_WIDTH * vn.x)),
					(edge.y + cpa.getIntensity() * scaleFactor
							* (_RNA.CHEM_PROB_TRIANGLE_WIDTH * vn.y)));
			Point2D.Double arrowTip2 = new Point2D.Double(
					(edge.x + cpa.getIntensity() * scaleFactor
							* (-_RNA.CHEM_PROB_TRIANGLE_WIDTH * vn.x)),
					(edge.y + cpa.getIntensity() * scaleFactor
							* (-_RNA.CHEM_PROB_TRIANGLE_WIDTH * vn.y)));
			GeneralPath p2 = new GeneralPath();
			p2.moveTo((float) base.x, (float) base.y);
			p2.lineTo((float) arrowTip1.x, (float) arrowTip1.y);
			p2.lineTo((float) arrowTip2.x, (float) arrowTip2.y);
			p2.closePath();
			g2D.fill(p2);
		}
			break;
		case DOT: {
			Double radius = scaleFactor * _RNA.CHEM_PROB_DOT_RADIUS
					* cpa.getIntensity();
			Point2D.Double center = new Point2D.Double((base.x + radius * v.x),
					(base.y + radius * v.y));
			g2D.fillCircle((center.x - radius), (center.y - radius),
					(2 * radius));
		}
			break;
		}
	}

	Point2D.Double buildCaptionPosition(ModeleBase mb, double scaleFactor,
			double heightEstimate) {
		double radius = 2.0;
		if (_RNA.isNumberDrawn(mb, getNumPeriod())) {
			radius += _conf._distNumbers;
		}
		Point2D.Double center = mb.getCenter();
		Point2D.Double p = mb.getCoords();
		double realDistance = _RNA.BASE_RADIUS * radius + heightEstimate;
		return new Point2D.Double(center.getX() + (p.getX() - center.getX())
				* ((p.distance(center) + realDistance) / p.distance(center)),
				center.getY()
						+ (p.getY() - center.getY())
						* ((p.distance(center) + realDistance) / p
								.distance(center)));
	}

	private void renderAnnotations(VueVARNAGraphics g2D, double offX,
			double offY, double rnaBBoxX, double rnaBBoxY, double scaleFactor) {
		for (TextAnnotation textAnnotation : _RNA.getAnnotations()) {
			g2D.setColor(textAnnotation.getColor());
			g2D.setFont(textAnnotation
					.getFont()
					.deriveFont(
							(float) (2.0 * textAnnotation.getFont().getSize() * scaleFactor)));
			Point2D.Double position = textAnnotation.getCenterPosition();
			if (textAnnotation.getType() == TextAnnotation.AnchorType.BASE) {
				ModeleBase mb = (ModeleBase) textAnnotation.getAncrage();
				double fontHeight = Math.ceil(textAnnotation.getFont()
						.getSize());
				position = buildCaptionPosition(mb, scaleFactor, fontHeight);
			}
			position = transformCoord(position, offX, offY, rnaBBoxX, rnaBBoxY,
					scaleFactor);
			g2D.drawStringCentered(textAnnotation.getTexte(), position.x,
					position.y);
			if ((_selectedAnnotation == textAnnotation)
					&& (_highlightAnnotation)) {
				drawStringOutline(g2D, textAnnotation.getTexte(), position.x,
						position.y, 5);
			}
		}
		for (ChemProbAnnotation cpa : _RNA.getChemProbAnnotations()) {
			Point2D.Double anchor = transformCoord(cpa.getAnchorPosition(),
					offX, offY, rnaBBoxX, rnaBBoxY, scaleFactor);
			drawChemProbAnnotation(g2D, cpa, anchor, scaleFactor);
		}

	}

	public Rectangle2D.Double getExtendedRNABBox() {
		// We get the logical bounding box
		Rectangle2D.Double rnabbox = _RNA.getBBox();
		rnabbox.y -= _conf._distNumbers * _RNA.BASE_RADIUS;
		rnabbox.height += 2.0 * _conf._distNumbers * _RNA.BASE_RADIUS;
		rnabbox.x -= _conf._distNumbers * _RNA.BASE_RADIUS;
		rnabbox.width += 2.0 * _conf._distNumbers * _RNA.BASE_RADIUS;
		if (_RNA.hasVirtualLoops()) {
			rnabbox.y -= RNA.VIRTUAL_LOOP_RADIUS;
			rnabbox.height += 2.0 * RNA.VIRTUAL_LOOP_RADIUS;
			rnabbox.x -= RNA.VIRTUAL_LOOP_RADIUS;
			rnabbox.width += 2.0 * RNA.VIRTUAL_LOOP_RADIUS;
		}
		return rnabbox;
	}

	public void drawBackbone(VueVARNAGraphics g2D, Point2D.Double[] newCoords,
			double newRadius, double _scaleFactor) {
		// Drawing backbone
		if (getDrawBackbone()) {
			g2D.setStrokeThickness(1.5 * _scaleFactor);
			g2D.setColor(_conf._backboneColor);
			
			ModeleBackbone bck = _RNA.getBackbone();


			for (int i = 1; i < _RNA.get_listeBases().size(); i++) {
				Point2D.Double p1 = newCoords[i - 1];
				Point2D.Double p2 = newCoords[i];
				double dist = p1.distance(p2);
				int a = _RNA.getBaseAt(i - 1).getElementStructure();
				int b = _RNA.getBaseAt(i).getElementStructure();
				boolean consecutivePair = (a == i) && (b == i - 1);

				if ((dist > 0)) {
					Point2D.Double vbp = new Point2D.Double();
					vbp.x = (p2.x - p1.x) / dist;
					vbp.y = (p2.y - p1.y) / dist;
					
					BackboneType bt = bck.getTypeBefore(i);
					if (bt!=BackboneType.DISCONTINUOUS_TYPE)
					{
						if (bt==BackboneType.MISSING_PART_TYPE) {
							g2D.setSelectionStroke();
						} else {
							g2D.setPlainStroke();
						}
						g2D.setColor(bck.getColorBefore(i, _conf._backboneColor));
						
						if (consecutivePair
								&& (_RNA.getDrawMode() != RNA.DRAW_MODE_LINEAR)
								&& (_RNA.getDrawMode() != RNA.DRAW_MODE_CIRCULAR)) {
							int dir = 0;
							if (i + 1 < newCoords.length) {
								dir = (_RNA.testDirectionality(i - 1, i, i + 1) ? -1
										: 1);
							} else if (i - 2 >= 0) {
								dir = (_RNA.testDirectionality(i - 2, i - 1, i) ? -1
										: 1);
							}
							Point2D.Double vn = new Point2D.Double(dir * vbp.y,
									-dir * vbp.x);
							Point2D.Double centerSeg = new Point2D.Double(
									(p1.x + p2.x) / 2.0, (p1.y + p2.y) / 2.0);
							double distp1CenterSeq = p1.distance(centerSeg);
							double centerDist = Math
									.sqrt((RNA.VIRTUAL_LOOP_RADIUS * _scaleFactor
											* RNA.VIRTUAL_LOOP_RADIUS * _scaleFactor)
											- distp1CenterSeq * distp1CenterSeq);
							Point2D.Double centerLoop = new Point2D.Double(
									centerSeg.x + centerDist * vn.x, centerSeg.y
											+ centerDist * vn.y);
							double radius = centerLoop.distance(p1);
							double a1 = 360.
									* (Math.atan2(-(p1.y - centerLoop.y),
											(p1.x - centerLoop.x)))
									/ (2. * Math.PI);
							double a2 = 360.
									* (Math.atan2(-(p2.y - centerLoop.y),
											(p2.x - centerLoop.x)))
									/ (2. * Math.PI);
							double angle = (a2 - a1);
							if (-dir * angle < 0) {
								angle += -dir * 360.;
							}
							// if (angle<0.) angle += 360.;
							// angle = -dir*(360-dir*angle);
							g2D.drawArc(centerLoop.x + .8 * newRadius * vn.x,
									centerLoop.y + .8 * newRadius * vn.y,
									2 * radius, 2 * radius, a1, angle);
						} else {
							g2D.drawLine((newCoords[i - 1].x + newRadius * vbp.x),
									(newCoords[i - 1].y + newRadius * vbp.y),
									(newCoords[i].x - newRadius * vbp.x),
									(newCoords[i].y - newRadius * vbp.y));
						}
					}
				}
			}
		}
	}

	public Point2D.Double logicToPanel(Point2D.Double logicPoint) {
		return new Point2D.Double(_offX
				+ (getScaleFactor() * (logicPoint.x - _offsetRNA.x)), _offY
				+ (getScaleFactor() * (logicPoint.y - _offsetRNA.y)));

	}

	public Rectangle2D.Double renderRNA(VueVARNAGraphics g2D,
			Rectangle2D.Double bbox) {
		return renderRNA(g2D, bbox, false, true);
	}

	private double computeScaleFactor(Rectangle2D.Double bbox,
			boolean localView, boolean autoCenter) {
		Rectangle2D.Double rnabbox = getExtendedRNABBox();
		double scaleFactor = Math.min((double) bbox.width
				/ (double) rnabbox.width, (double) bbox.height
				/ (double) rnabbox.height);

		// Use it to get an estimate of the font size for numbers ...
		float newFontSize = Math.max(1,
				(int) ((1.7 * _RNA.BASE_RADIUS) * scaleFactor));
		// ... and increase bounding box accordingly
		rnabbox.y -= newFontSize;
		rnabbox.height += newFontSize;
		if (_conf._drawColorMap) {
			rnabbox.height += getColorMapHeight();
		}
		rnabbox.x -= newFontSize;
		rnabbox.width += newFontSize;

		// Now, compute the final scaling factor and corresponding font size
		scaleFactor = Math.min((double) bbox.width / (double) rnabbox.width,
				(double) bbox.height / (double) rnabbox.height);
		if (localView) {
			if (_conf._autoFit)
				setScaleFactor(scaleFactor);
			scaleFactor = getScaleFactor();
		}
		return scaleFactor;
	}

	public synchronized Rectangle2D.Double renderRNA(VueVARNAGraphics g2D,
			Rectangle2D.Double bbox, boolean localView, boolean autoCenter) {
		Rectangle2D.Double rnaMultiBox = new Rectangle2D.Double(0, 0, 1, 1);
		double scaleFactor = computeScaleFactor(bbox, localView, autoCenter);
		float newFontSize = Math.max(1,
				(int) ((1.7 * _RNA.BASE_RADIUS) * scaleFactor));
		double newRadius = Math.max(1.0, (scaleFactor * _RNA.BASE_RADIUS));
		setBaseFontSize(newFontSize);
		setNumbersFontSize(newFontSize);
		double offX = bbox.x;
		double offY = bbox.y;
		Rectangle2D.Double rnabbox = getExtendedRNABBox();

		if (_RNA.getSize() != 0) {

			Point2D.Double offsetRNA = new Point2D.Double(rnabbox.x, rnabbox.y);

			if (autoCenter) {
				offX = (bbox.x + (bbox.width - Math.round(rnabbox.width
						* scaleFactor)) / 2.0);
				offY = (bbox.y + (bbox.height - Math.round(rnabbox.height
						* scaleFactor)) / 2.0);
				if (localView) {
					_offX = offX;
					_offY = offY;
					_offsetPanel = new Point2D.Double(_offX, _offY);
					_offsetRNA = new Point2D.Double(rnabbox.x, rnabbox.y);
				}
			}

			if (localView) {
				offX = _offX;
				offY = _offY;
				offsetRNA = _offsetRNA;
			}

			// Re-scaling once and for all
			Point2D.Double[] newCoords = new Point2D.Double[_RNA
					.get_listeBases().size()];
			Point2D.Double[] newCenters = new Point2D.Double[_RNA
					.get_listeBases().size()];
			for (int i = 0; i < _RNA.get_listeBases().size(); i++) {
				ModeleBase mb = _RNA.getBaseAt(i);
				newCoords[i] = new Point2D.Double(offX
						+ (scaleFactor * (mb.getCoords().x - offsetRNA.x)),
						offY + (scaleFactor * (mb.getCoords().y - offsetRNA.y)));

				Point2D.Double centerBck = _RNA.getCenter(i);
				// si la base est dans un angle entre une boucle et une helice
				if (_RNA.get_drawMode() == RNA.DRAW_MODE_NAVIEW
						|| _RNA.get_drawMode() == RNA.DRAW_MODE_RADIATE) {
					if ((mb.getElementStructure() != -1)
							&& i < _RNA.get_listeBases().size() - 1 && i > 1) {
						ModeleBase b1 = _RNA.get_listeBases().get(i - 1);
						ModeleBase b2 = _RNA.get_listeBases().get(i + 1);
						int j1 = b1.getElementStructure();
						int j2 = b2.getElementStructure();
						if ((j1 == -1) ^ (j2 == -1)) {
							// alors la position du nombre associé doit etre
							Point2D.Double a1 = b1.getCoords();
							Point2D.Double a2 = b2.getCoords();
							Point2D.Double c1 = b1.getCenter();
							Point2D.Double c2 = b2.getCenter();

							centerBck.x = mb.getCoords().x + (c1.x - a1.x)
									/ c1.distance(a1) + (c2.x - a2.x)
									/ c2.distance(a2);
							centerBck.y = mb.getCoords().y + (c1.y - a1.y)
									/ c1.distance(a1) + (c2.y - a2.y)
									/ c2.distance(a2);
						}
					}
				}
				newCenters[i] = new Point2D.Double(offX
						+ (scaleFactor * (centerBck.x - offsetRNA.x)), offY
						+ (scaleFactor * (centerBck.y - offsetRNA.y)));
			}
			// Keep track of coordinates for mouse interactions
			if (localView) {
				_realCoords = newCoords;
				_realCenters = newCenters;
			}

			g2D.setStrokeThickness(1.5 * scaleFactor);
			g2D.setPlainStroke();
			g2D.setFont(_conf._fontBasesGeneral);

			// Drawing region highlights Annotation
			drawRegionHighlightsAnnotation(g2D, _realCoords, _realCenters,
					scaleFactor);
			drawBackbone(g2D, newCoords, newRadius, scaleFactor);

			// Drawing base-pairs
			// pour chaque base
			for (int i = 0; i < _RNA.get_listeBases().size(); i++) {
				int j = _RNA.get_listeBases().get(i).getElementStructure();
				// si c'est une parenthese ouvrante (premiere base du
				// couple)
				if (j > i) {
					ModeleBP msbp = _RNA.get_listeBases().get(i).getStyleBP();
					// System.err.println(msbp);
					if (msbp.isCanonical() || _conf._drawnNonCanonicalBP) {
						if (_RNA.get_drawMode() == RNA.DRAW_MODE_LINEAR) {
							g2D.setStrokeThickness(_RNA.getBasePairThickness(
									msbp, _conf)
									* 2.0
									* scaleFactor
									* _conf._bpThickness);
						} else {
							g2D.setStrokeThickness(_RNA.getBasePairThickness(
									msbp, _conf) * 1.5 * scaleFactor);
						}
						g2D.setColor(_RNA.getBasePairColor(msbp, _conf));

						if (_RNA.get_drawMode() == RNA.DRAW_MODE_LINEAR) {
							drawBasePairArc(g2D, i, j, newCoords[i],
									newCoords[j], scaleFactor, msbp, newRadius);
						} else {
							drawBasePair(g2D, newCoords[i], newCoords[j], msbp,
									newRadius, scaleFactor);
						}
					}
				}
			}

			// Liaisons additionelles (non planaires)
			if (_conf._drawnNonPlanarBP) {
				ArrayList<ModeleBP> bpaux = _RNA.getStructureAux();
				for (int k = 0; k < bpaux.size(); k++) {
					ModeleBP msbp = bpaux.get(k);
					if (msbp.isCanonical() || _conf._drawnNonCanonicalBP) {
						int i = msbp.getPartner5().getIndex();
						int j = msbp.getPartner3().getIndex();
						if (_RNA.get_drawMode() == RNA.DRAW_MODE_LINEAR) {
							g2D.setStrokeThickness(_RNA.getBasePairThickness(
									msbp, _conf)
									* 2.5
									* scaleFactor
									* _conf._bpThickness);
							g2D.setPlainStroke();
						} else {
							g2D.setStrokeThickness(_RNA.getBasePairThickness(
									msbp, _conf) * 1.5 * scaleFactor);
							g2D.setPlainStroke();
						}

						g2D.setColor(_RNA.getBasePairColor(msbp, _conf));
						if (j > i) {
							if (_RNA.get_drawMode() == RNA.DRAW_MODE_LINEAR) {
								drawBasePairArc(g2D, i, j, newCoords[i],
										newCoords[j], scaleFactor, msbp, newRadius);
							} else {
								drawBasePair(g2D, newCoords[i], newCoords[j],
										msbp, newRadius, scaleFactor);
							}
						}
					}
				}
			}

			// Drawing bases
			g2D.setPlainStroke();
			for (int i = 0; i < Math.min(_RNA.get_listeBases().size(),
					newCoords.length); i++) {
				drawBase(g2D, i, newCoords, newCenters, newRadius, scaleFactor,
						localView);
			}

			rnaMultiBox = new Rectangle2D.Double(offX, offY,
					(scaleFactor * rnabbox.width) - 1,
					(scaleFactor * rnabbox.height) - 1);

			if (localView) {
				// Drawing bbox
				if (_debug || _drawBBox) {
					g2D.setColor(Color.RED);
					g2D.setSelectionStroke();
					g2D.drawRect(rnaMultiBox.x, rnaMultiBox.y,
							rnaMultiBox.width, rnaMultiBox.height);
				}

				// Draw color map
				if (_conf._drawColorMap) {
					drawColorMap(g2D, scaleFactor, rnabbox);
				}

				if (_debug || _drawBBox) {
					g2D.setColor(Color.GRAY);
					g2D.setSelectionStroke();
					g2D.drawRect(0, 0, getWidth() - 1, getHeight()
							- getTitleHeight() - 1);
				}
			}
			// Draw annotations
			renderAnnotations(g2D, offX, offY, offsetRNA.x, offsetRNA.y,
					scaleFactor);
			// Draw additional debug shape
			if (_RNA._debugShape != null) {
				Color c = new Color(255, 0, 0, 50);
				g2D.setColor(c);
				AffineTransform at = new AffineTransform();
				at.translate(offX - scaleFactor * rnabbox.x, offY - scaleFactor
						* rnabbox.y);
				at.scale(scaleFactor, scaleFactor);
				Shape s = at.createTransformedShape(_RNA._debugShape);
				if (s instanceof GeneralPath) {
					g2D.fill((GeneralPath) s);
				}
			}
		} else {
			g2D.setColor(VARNAConfig.DEFAULT_MESSAGE_COLOR);
			g2D.setFont(VARNAConfig.DEFAULT_MESSAGE_FONT);
			rnaMultiBox = new Rectangle2D.Double(0,0,10,10);
			g2D.drawStringCentered("No RNA here", bbox.getCenterX(),bbox.getCenterY());
		}
		return rnaMultiBox;
	}

	public void centerViewOn(double x, double y) {
		Rectangle2D.Double r = _RNA.getBBox();
		_target = new Point2D.Double(x, y);
		Point2D.Double q = logicToPanel(_target);
		Point p = new Point((int) (-q.x), (int) (-q.y));
		setTranslation(p);
		repaint();
	}

	Point2D.Double _target = new Point2D.Double(0, 0);
	Point2D.Double _target2 = new Point2D.Double(0, 0);

	public ModeleBase getBaseAt(Point2D.Double po) {
		ModeleBase mb = null;
		Point2D.Double p = panelToLogicPoint(po);
		double dist = Double.MAX_VALUE;
		for (ModeleBase tmp : _RNA.get_listeBases()) {
			double ndist = tmp.getCoords().distance(p);
			if (dist > ndist) {
				mb = tmp;
				dist = ndist;
			}
		}
		return mb;
	}

	public void setColorMapValues(Double[] values) {
		_RNA.setColorMapValues(values, _conf._cm, true);
		_conf._drawColorMap = true;
		repaint();
	}

	public void setColorMapMaxValue(double d) {
		_conf._cm.setMaxValue(d);
	}

	public void setColorMapMinValue(double d) {
		_conf._cm.setMinValue(d);
	}

	public ModeleColorMap getColorMap() {
		return _conf._cm;
	}

	public void setColorMap(ModeleColorMap cm) {
		//_RNA.adaptColorMapToValues(cm);
		_conf._cm = cm;
		repaint();
	}

	public void setColorMapCaption(String caption) {
		_conf._colorMapCaption = caption;
		repaint();
	}

	public String getColorMapCaption() {
		return _conf._colorMapCaption;
	}

	public void drawColorMap(boolean draw) {
		_conf._drawColorMap = draw;
	}

	private double getColorMapHeight() {
		double result = VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE
				+ _conf._colorMapHeight;
		if (!_conf._colorMapCaption.equals(""))
			result += VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE;
		return result;
	}

	private void drawColorMap(VueVARNAGraphics g2D, double scaleFactor,
			Rectangle2D.Double rnabbox) {
		double v1 = _conf._cm.getMinValue();
		double v2 = _conf._cm.getMaxValue();
		double x, y;
		g2D.setPlainStroke();

		double xSpaceAvail = 0;
		double ySpaceAvail = Math
				.min((getHeight() - rnabbox.height * scaleFactor - getTitleHeight()) / 2.0,
						scaleFactor
								* (_conf._colorMapHeight + VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE));
		if ((int) ySpaceAvail == 0) {
			xSpaceAvail = Math.min(
					(getWidth() - rnabbox.width * scaleFactor) / 2, scaleFactor
							* (_conf._colorMapWidth)
							+ VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH);
		}
		double xBase = (xSpaceAvail + _offX + scaleFactor
				* (rnabbox.width - _conf._colorMapWidth - _conf._colorMapXOffset));
		double hcaption = VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE;
		double yBase = (ySpaceAvail + _offY + scaleFactor
				* (rnabbox.height - _conf._colorMapHeight
						- _conf._colorMapYOffset - hcaption));

		for (int i = 0; i < _conf._colorMapWidth; i++) {
			double ratio = (((double) i) / ((double) _conf._colorMapWidth));
			double val = v1 + (v2 - v1) * ratio;
			g2D.setColor(_conf._cm.getColorForValue(val));
			x = (xBase + scaleFactor * i);
			y = yBase;
			g2D.fillRect(x, y, scaleFactor
					* VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH,
					(scaleFactor * _conf._colorMapHeight));
		}
		g2D.setColor(VARNAConfig.DEFAULT_COLOR_MAP_OUTLINE);
		g2D.drawRect(xBase, yBase,
				(VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH - 1 + scaleFactor
						* _conf._colorMapWidth),
				((scaleFactor * _conf._colorMapHeight)));
		g2D.setFont(getFont()
				.deriveFont(
						(float) (scaleFactor * VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE)));
		g2D.setColor(VARNAConfig.DEFAULT_COLOR_MAP_FONT_COLOR);
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(0);
		g2D.drawStringCentered(nf.format(_conf._cm.getMinValue()), xBase, 
				yBase
				+ scaleFactor * (_conf._colorMapHeight+(VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE / 1.7)));
		g2D.drawStringCentered(nf.format(_conf._cm.getMaxValue()), xBase
				+ VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH + scaleFactor
				* _conf._colorMapWidth, 
				yBase
				+ scaleFactor * (_conf._colorMapHeight+(VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE / 1.7)));
		if (!_conf._colorMapCaption.equals(""))
			g2D.drawStringCentered(
					"" + _conf._colorMapCaption,
					xBase + scaleFactor * _conf._colorMapWidth / 2.0,
					yBase
							+ scaleFactor
							* (VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE / 1.7 + _conf._colorMapHeight));

	}

	public Point2D.Double panelToLogicPoint(Point2D.Double p) {
		return new Point2D.Double(
				((p.x - getOffsetPanel().x) / getScaleFactor())
						+ getRNAOffset().x,
				((p.y - getOffsetPanel().y) / getScaleFactor())
						+ getRNAOffset().y);
	}

	public Point2D.Double transformCoord(Point2D.Double coordDebut,
			double offX, double offY, double rnaBBoxX, double rnaBBoxY,
			double scaleFactor) {
		return new Point2D.Double(offX
				+ (scaleFactor * (coordDebut.x - rnaBBoxX)), offY
				+ (scaleFactor * (coordDebut.y - rnaBBoxY)));
	}

	public void eraseSequence() {
		_RNA.eraseSequence();
	}

	public Point2D.Double transformCoord(Point2D.Double coordDebut) {
		Rectangle2D.Double rnabbox = getExtendedRNABBox();
		return new Point2D.Double(_offX
				+ (getScaleFactor() * (coordDebut.x - rnabbox.x)), _offY
				+ (getScaleFactor() * (coordDebut.y - rnabbox.y)));
	}

	@Override
  public void paintComponent(Graphics g) {
		paintComponent(g, false);
	}

	public void paintComponent(Graphics g, boolean transparentBackground) {
		if (_premierAffichage) {
			// _border = new Dimension(0, 0);
			_translation.x = 0;
			_translation.y = (int) (-getTitleHeight() / 2.0);
			_popup.buildPopupMenu();
			//this.add(_popup);
			_premierAffichage = false;
		}

		Graphics2D g2 = (Graphics2D) g;
		Stroke dflt = g2.getStroke();
		VueVARNAGraphics g2D = new SwingGraphics(g2);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		//this.removeAll();
		super.paintComponent(g2);
		renderComponent(g2D, transparentBackground, getScaleFactor());
		if (isFocusOwner()) {
			g2.setStroke(new BasicStroke(1.5f));
			g2.setColor(Color.decode("#C0C0C0"));
			g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

		}
		g2.setStroke(dflt);
		/*
		 * PSExport e = new PSExport(); SecStrProducerGraphics export = new
		 * SecStrProducerGraphics(e); renderRNA(export, getExtendedRNABBox());
		 * try { export.saveToDisk("./out.ps"); } catch
		 * (ExceptionWritingForbidden e1) { e1.printStackTrace(); }
		 */
	}

	/**
	 * Draws current RNA structure in a given Graphics "device".
	 * 
	 * @param g2D
	 *            A graphical device
	 * @param transparentBackground
	 *            Whether the background should be transparent, or drawn.
	 */
	public synchronized void renderComponent(VueVARNAGraphics g2D,
			boolean transparentBackground, double scaleFactor) {

		updateTitleHeight();

		if (_debug || _drawBorder) {
			g2D.setColor(Color.BLACK);
			g2D.setPlainStroke();
			g2D.drawRect(getLeftOffset(), getTopOffset(), getInnerWidth(),
					getInnerHeight());

		}

		
		if (!transparentBackground) {
			super.setBackground(_conf._backgroundColor);
		} else {
			super.setBackground(new Color(0, 0, 0, 120));
		}

		if (getMinimumSize().height < getSize().height
				&& getMinimumSize().width < getSize().width) {
			// Draw Title
			if (!getTitle().equals("")) {
				g2D.setColor(_conf._titleColor);
				g2D.setFont(_conf._titleFont);
				g2D.drawStringCentered(getTitle(), this.getWidth() / 2,
						this.getHeight() - getTitleHeight() / 2.0);
			}
			// Draw RNA
			renderRNA(g2D, getClip(), true, _conf._autoCenter);
		}
		if (_selectionRectangle != null) {
			g2D.setColor(Color.BLACK);
			g2D.setSelectionStroke();
			g2D.drawRect(_selectionRectangle.x, _selectionRectangle.y,
					_selectionRectangle.width, _selectionRectangle.height);
		}
		if ((_linkOrigin != null) && (_linkDestination != null)) {
			g2D.setColor(_conf._bondColor);
			g2D.setPlainStroke();
			g2D.setStrokeThickness(3.0 * scaleFactor);
			Point2D.Double linkOrigin = (_linkOrigin);
			Point2D.Double linkDestination = (_linkDestination);
			g2D.drawLine(linkOrigin.x, linkOrigin.y, linkDestination.x,
					linkDestination.y);
			for (int i : getSelection().getIndices())
				drawBase(g2D, i, _realCoords, _realCenters, scaleFactor
						* _RNA.BASE_RADIUS, scaleFactor, true);
		}

		if (_debug) {
			g2D.setStrokeThickness(3.0 * scaleFactor);
			g2D.setColor(Color.black);
			Point2D.Double t = this.logicToPanel(_target);
			g2D.drawLine(t.x - 3, t.y - 3, t.x + 3, t.y + 3);
			g2D.drawLine(t.x - 3, t.y + 3, t.x + 3, t.y - 3);
			g2D.setColor(Color.red);
			t = this.logicToPanel(_target2);
			g2D.drawLine(t.x - 3, t.y - 3, t.x + 3, t.y + 3);
			g2D.drawLine(t.x - 3, t.y + 3, t.x + 3, t.y - 3);
		}
	}

	public void drawRegionHighlightsAnnotation(VueVARNAGraphics g2D,
			Point2D.Double[] realCoords, Point2D.Double[] realCenters,
			double scaleFactor) {
		g2D.setStrokeThickness(2.0 * scaleFactor);
		g2D.setPlainStroke();
		for (HighlightRegionAnnotation r : _RNA.getHighlightRegion()) {
			GeneralPath s = r.getShape(realCoords, realCenters, scaleFactor);
			g2D.setColor(r.getFillColor());
			g2D.fill(s);
			g2D.setColor(r.getOutlineColor());
			g2D.draw(s);
		}
	}

	private Rectangle2D.Double getClip() {
		return new Rectangle2D.Double(getLeftOffset(), getTopOffset(),
				this.getInnerWidth(), this.getInnerHeight());
	}

	public Rectangle2D.Double getViewClip() {
		return new Rectangle2D.Double(this.getLeftOffset(),
				this.getTopOffset(), this.getInnerWidth(),
				this.getInnerHeight());
	}

	/**
	 * Returns the color used to draw backbone bounds.
	 * 
	 * @return The color used to draw backbone bounds
	 */
	public Color getBackboneColor() {
		return _conf._backboneColor;
	}

	/**
	 * Sets the color to be used for drawing backbone interactions.
	 * 
	 * @param backbone_color
	 *            The new color for the backbone bounds
	 */
	public void setBackboneColor(Color backbone_color) {
		_conf._backboneColor = backbone_color;
	}

	/**
	 * Returns the color used to display hydrogen bonds (base pairings)
	 * 
	 * @return The color of hydrogen bonds
	 */
	public Color getBondColor() {
		return _conf._bondColor;
	}

	/**
	 * Returns the title of this panel
	 * 
	 * @return The title
	 */
	public String getTitle() {
		return _RNA.getName();
	}

	/**
	 * Sets the new color to be used for hydrogen bonds (base pairings)
	 * 
	 * @param bond_color
	 *            The new color for hydrogen bonds
	 */
	public void setDefaultBPColor(Color bond_color) {
		_conf._bondColor = bond_color;
	}

	/**
	 * Sets the size of the border, i.e. the empty space between the end of the
	 * drawing area and the actual border.
	 * 
	 * @param b
	 *            The new border size
	 */
	public void setBorderSize(Dimension b) {
		_border = b;
	}

	/**
	 * Returns the size of the border, i.e. the empty space between the end of
	 * the drawing area
	 * 
	 * @return The border size
	 */
	public Dimension getBorderSize() {
		return _border;
	}

	/**
	 * Sets the RNA to be displayed within this Panel. This method does not use
	 * a drawing algorithm to reassigns base coordinates, rather assuming that
	 * the RNA was previously drawn.
	 * 
	 * @param r
	 *            An already drawn RNA to display in this panel
	 */
	public synchronized void showRNA(RNA r) {
		fireUINewStructure(r);
		_RNA = r;
	}

	/**
	 * Sets the RNA secondary structure to be drawn in this panel, using the
	 * default layout algorithm. In addition to the raw nucleotides sequence,
	 * the secondary structure is given in the so-called "Dot-bracket notation"
	 * (DBN) format. This format is a well-parenthesized word over the alphabet
	 * '(',')','.'.<br/>
	 * Ex:<code>((((((((....))))..(((((...))).))))))</code><br />
	 * Returns <code>true</code> if the sequence/structure couple could be
	 * parsed into a valid secondary structure, and <code>false</code>
	 * otherwise.
	 * 
	 * @param seq
	 *            The raw nucleotides sequence
	 * @param str
	 *            The secondary structure
	 * @throws ExceptionNonEqualLength
	 */
	public void drawRNA(String seq, String str) throws ExceptionNonEqualLength {
		drawRNA(seq, str, _RNA.get_drawMode());
	}

	/**
	 * Sets the RNA secondary structure to be drawn in this panel, using a given
	 * layout algorithm.
	 * 
	 * @param r
	 *            The new secondary structure
	 * @param drawMode
	 *            The drawing algorithm
	 */
	public void drawRNA(RNA r, int drawMode) {
		r.setDrawMode(drawMode);
		drawRNA(r);
	}

	/**
	 * Redraws the current RNA. This reassigns base coordinates to their default
	 * value using the current drawing algorithm.
	 */

	public void drawRNA() {
		try {
			_RNA.drawRNA(_RNA.get_drawMode(), _conf);
		} catch (ExceptionNAViewAlgorithm e) {
			errorDialog(e);
			e.printStackTrace();
		}
		repaint();
	}

	/**
	 * Sets the RNA secondary structure to be drawn in this panel, using the
	 * current drawing algorithm.
	 * 
	 * @param r
	 *            The new secondary structure
	 */
	public void drawRNA(RNA r) {
		if (r != null) {
			_RNA = r;
			drawRNA();
		}
	}

	/**
	 * Sets the RNA secondary structure to be drawn in this panel, using a given
	 * layout algorithm. In addition to the raw nucleotides sequence, the
	 * secondary structure is given in the so-called "Dot-bracket notation"
	 * (DBN) format. This format is a well-parenthesized word over the alphabet
	 * '(',')','.'.<br/>
	 * Ex: <code>((((((((....))))..(((((...))).))))))</code><br />
	 * Returns <code>true</code> if the sequence/structure couple could be
	 * parsed into a valid secondary structure, and <code>false</code>
	 * otherwise.
	 * 
	 * @param seq
	 *            The raw nucleotides sequence
	 * @param str
	 *            The secondary structure
	 * @param drawMode
	 *            The drawing algorithm
	 * @throws ExceptionNonEqualLength
	 */
	public void drawRNA(String seq, String str, int drawMode)
			throws ExceptionNonEqualLength {
		_RNA.setDrawMode(drawMode);
		try {
			_RNA.setRNA(seq, str);
			drawRNA();
		} catch (ExceptionUnmatchedClosingParentheses e) {
			errorDialog(e);
		} catch (ExceptionFileFormatOrSyntax e1) {
			errorDialog(e1);
		}
	}

	public void drawRNA(Reader r, int drawMode) throws ExceptionNonEqualLength,
			ExceptionFileFormatOrSyntax {
		_RNA.setDrawMode(drawMode);
		Collection<RNA> rnas = RNAFactory.loadSecStr(r);
		if (rnas.isEmpty()) {
			throw new ExceptionFileFormatOrSyntax(
					"No RNA could be parsed from that source.");
		}
		_RNA = rnas.iterator().next();
		drawRNA();
	}

	/**
	 * Draws a secondary structure of RNA using the default drawing algorithm
	 * and displays it, using an interpolated transition between the previous
	 * one and the new one. Extra bases, resulting from a size difference
	 * between the two successive RNAs, are assumed to initiate from the middle
	 * of the sequence. In other words, both prefixes and suffixes of the RNAs
	 * are assumed to match, and what remains is an insertion.
	 * 
	 * @param seq
	 *            Sequence
	 * @param str
	 *            Structure in dot bracket notation
	 * @throws ExceptionNonEqualLength
	 *             If len(seq)!=len(str)
	 */
	public void drawRNAInterpolated(String seq, String str)
			throws ExceptionNonEqualLength {
		drawRNAInterpolated(seq, str, _RNA.get_drawMode());
	}

	/**
	 * Draws a secondary structure of RNA using a given algorithm and displays
	 * it, using an interpolated transition between the previous one and the new
	 * one. Extra bases, resulting from a size difference between the two
	 * successive RNAs, are assumed to initiate from the middle of the sequence.
	 * In other words, both prefixes and suffixes of the RNAs are assumed to
	 * match, and what remains is an insertion.
	 * 
	 * @param seq
	 *            Sequence
	 * @param str
	 *            Structure in dot bracket notation
	 * @param drawMode
	 *            The drawing algorithm to be used for the initial placement
	 * @throws ExceptionNonEqualLength
	 *             If len(seq)!=len(str)
	 */
	public void drawRNAInterpolated(String seq, String str, int drawMode) {
		drawRNAInterpolated(seq, str, drawMode,
				Mapping.DefaultOutermostMapping(_RNA.get_listeBases().size(),
						str.length()));
	}

	/**
	 * Draws a secondary structure of RNA using the default drawing algorithm
	 * and displays it, using an interpolated transition between the previous
	 * one and the new one. Here, a mapping between those bases of the new
	 * structure and the previous one is explicitly provided.
	 * 
	 * @param seq
	 *            Sequence
	 * @param str
	 *            Structure in dot bracket notation
	 * @param m
	 *            A mapping between the currently rendered structure and its
	 *            successor (seq,str)
	 * @throws ExceptionNonEqualLength
	 *             If len(seq)!=len(str)
	 */
	public void drawRNAInterpolated(String seq, String str, Mapping m) {
		drawRNAInterpolated(seq, str, _RNA.get_drawMode(), m);
	}

	/**
	 * Draws a secondary structure of RNA using a given drawing algorithm and
	 * displays it, using an interpolated transition between the previous one
	 * and the new one. Here, a mapping between those bases of the new structure
	 * and the previous one is provided.
	 * 
	 * @param seq
	 *            Sequence
	 * @param str
	 *            Structure in dot bracket notation
	 * @param drawMode
	 *            The drawing algorithm to be used for the initial placement
	 * @param m
	 *            A mapping between the currently rendered structure and its
	 *            successor (seq,str)
	 */
	public void drawRNAInterpolated(String seq, String str, int drawMode,
			Mapping m) {
		RNA target = new RNA();
		try {
			target.setRNA(seq, str);
			drawRNAInterpolated(target, drawMode, m);
		} catch (ExceptionUnmatchedClosingParentheses e) {
			errorDialog(e);
		} catch (ExceptionFileFormatOrSyntax e) {
			errorDialog(e);
		}
	}

	/**
	 * Draws a secondary structure of RNA using the default drawing algorithm
	 * and displays it, using an interpolated transition between the previous
	 * one and the new one. Here, a mapping between those bases of the new
	 * structure and the previous one is explicitly provided.
	 * 
	 * @param target
	 *            Secondary structure
	 */
	public void drawRNAInterpolated(RNA target) {
		drawRNAInterpolated(target, target.get_drawMode(),
				Mapping.DefaultOutermostMapping(_RNA.get_listeBases().size(),
						target.getSize()));
	}

	/**
	 * Draws a secondary structure of RNA using the default drawing algorithm
	 * and displays it, using an interpolated transition between the previous
	 * one and the new one. Here, a mapping between those bases of the new
	 * structure and the previous one is explicitly provided.
	 * 
	 * @param target
	 *            Secondary structure
	 * @param m
	 *            A mapping between the currently rendered structure and its
	 *            successor (seq,str)
	 */
	public void drawRNAInterpolated(RNA target, Mapping m) {
		drawRNAInterpolated(target, target.get_drawMode(), m);
	}

	/**
	 * Draws a secondary structure of RNA using a given drawing algorithm and
	 * displays it, using an interpolated transition between the previous one
	 * and the new one. Here, a mapping between those bases of the new structure
	 * and the previous one is provided.
	 * 
	 * @param target
	 *            Secondary structure of RNA
	 * @param drawMode
	 *            The drawing algorithm to be used for the initial placement
	 * @param m
	 *            A mapping between the currently rendered structure and its
	 *            successor (seq,str)
	 */
	public void drawRNAInterpolated(RNA target, int drawMode, Mapping m) {
		try {
			target.drawRNA(drawMode, _conf);
			_conf._drawColorMap = false;
			_interpolator.addTarget(target, m);
		} catch (ExceptionNAViewAlgorithm e) {
			errorDialog(e);
			e.printStackTrace();
		}
	}

	/**
	 * Returns the current algorithm used for drawing the structure
	 * 
	 * @return The current drawing algorithm
	 */
	public int getDrawMode() {
		return this._RNA.getDrawMode();
	}

	public void showRNA(RNA t, VARNAConfig cfg) {
		showRNA(t);
		if (cfg != null) {
			this.setConfig(cfg);
		}
		repaint();
	}

	/**
	 * Checks whether an interpolated transition bewteen two RNAs is occurring.
	 * 
	 * @return True if an interpolated transition is occurring, false otherwise
	 */

	public boolean isInterpolationInProgress() {
		if (_interpolator == null) {
			return false;
		} else
			return _interpolator.isInterpolationInProgress();
	}

	/**
	 * Simply displays (does not redraw) a secondary structure , using an
	 * interpolated transition between the previous one and the new one. A
	 * default mapping between those bases of the new structure and the previous
	 * one is used.
	 * 
	 * @param target
	 *            Secondary structure of RNA
	 */
	public void showRNAInterpolated(RNA target) {
		showRNAInterpolated(target, Mapping.DefaultOutermostMapping(_RNA
				.get_listeBases().size(), target.getSize()));
	}

	/**
	 * Simply displays (does not redraw) a secondary structure , using an
	 * interpolated transition between the previous one and the new one. Here, a
	 * mapping between bases of the new structure and the previous one is given.
	 * 
	 * @param target
	 *            Secondary structure of RNA
	 * @param m
	 *            A mapping between the currently rendered structure and its
	 *            successor (seq,str)
	 * @throws ExceptionNonEqualLength
	 *             If len(seq)!=len(str)
	 */
	public void showRNAInterpolated(RNA target, Mapping m) {
		showRNAInterpolated(target, null, m);
	}

	public void showRNAInterpolated(RNA target, VARNAConfig cfg, Mapping m) {
		_interpolator.addTarget(target, cfg, m);
	}

	/**
	 * When comparison mode is ON, sets the two RNA secondary structure to be
	 * drawn in this panel, using a given layout algorithm. In addition to the
	 * raw nucleotides sequence, the secondary structure is given in the
	 * so-called "Dot-bracket notation" (DBN) format. This format is a
	 * well-parenthesized word over the alphabet '(',')','.'.<br/>
	 * Ex: <code>((((((((....))))..(((((...))).))))))</code><br />
	 * 
	 * @param firstSeq
	 *            The first RNA raw nucleotides sequence
	 * @param firstStruct
	 *            The first RNA secondary structure
	 * @param secondSeq
	 *            The second RNA raw nucleotides sequence
	 * @param secondStruct
	 *            The second RNA secondary structure
	 * @param drawMode
	 *            The drawing algorithm
	 */
	public void drawRNA(String firstSeq, String firstStruct, String secondSeq,
			String secondStruct, int drawMode) {
		_RNA.setDrawMode(drawMode);
		/**
		 * Checking the sequences and structures validities...
		 */

		// This is a comparison, so the two RNA alignment past in parameters
		// must
		// have the same sequence and structure length.
		if (firstSeq.length() == secondSeq.length()
				&& firstStruct.length() == secondStruct.length()) {
			// First RNA
			if (firstSeq.length() != firstStruct.length()) {
				if (_conf._showWarnings) {
					emitWarning("First sequence length " + firstSeq.length()
							+ " differs from that of it's secondary structure "
							+ firstStruct.length()
							+ ". \nAdapting first sequence length ...");
				}
				if (firstSeq.length() < firstStruct.length()) {
					while (firstSeq.length() < firstStruct.length()) {
						firstSeq += " ";
					}
				} else {
					firstSeq = firstSeq.substring(0, firstStruct.length());
				}
			}

			// Second RNA
			if (secondSeq.length() != secondStruct.length()) {
				if (_conf._showWarnings) {
					emitWarning("Second sequence length " + secondSeq.length()
							+ " differs from that of it's secondary structure "
							+ secondStruct.length()
							+ ". \nAdapting second sequence length ...");
				}
				if (secondSeq.length() < secondStruct.length()) {
					while (secondSeq.length() < secondStruct.length()) {
						secondSeq += " ";
					}
				} else {
					secondSeq = secondSeq.substring(0, secondStruct.length());
				}
			}

			int RNALength = firstSeq.length();
			String string_superStruct = new String("");
			String string_superSeq = new String("");
			/**
			 * In this array, we'll have for each indexes of each characters of
			 * the final super-structure, the RNA number which is own it.
			 */
			ArrayList<Integer> array_rnaOwn = new ArrayList<Integer>();

			/**
			 * Generating super-structure sequences and structures...
			 */

			firstStruct = firstStruct.replace('-', '.');
			secondStruct = secondStruct.replace('-', '.');
			// First of all, we make the structure
			for (int i = 0; i < RNALength; i++) {
				// If both characters are the same, so it'll be in the super
				// structure
				if (firstStruct.charAt(i) == secondStruct.charAt(i)) {
					string_superStruct = string_superStruct
							+ firstStruct.charAt(i);
					array_rnaOwn.add(0);
				}
				// Else if one of the characters is an opening parenthese, so
				// it'll be an opening parenthese in the super structure
				else if (firstStruct.charAt(i) == '('
						|| secondStruct.charAt(i) == '(') {
					string_superStruct = string_superStruct + '(';
					array_rnaOwn.add((firstStruct.charAt(i) == '(') ? 1 : 2);
				}
				// Else if one of the characters is a closing parenthese, so
				// it'll be a closing parenthese in the super structure
				else if (firstStruct.charAt(i) == ')'
						|| secondStruct.charAt(i) == ')') {
					string_superStruct = string_superStruct + ')';
					array_rnaOwn.add((firstStruct.charAt(i) == ')') ? 1 : 2);
				} else {
					string_superStruct = string_superStruct + '.';
					array_rnaOwn.add(-1);
				}
			}

			// Next, we make the sequence taking the characters at the same
			// index in the first and second sequence
			for (int i = 0; i < RNALength; i++) {
				string_superSeq = string_superSeq + firstSeq.charAt(i)
						+ secondSeq.charAt(i);
			}

			// Now, we need to create the super-structure RNA with the owning
			// bases array
			// in order to color bases outer depending on the owning statement
			// of each bases.
			if (!string_superSeq.equals("") && !string_superStruct.equals("")) {
				try {
					_RNA.setRNA(string_superSeq, string_superStruct,
							array_rnaOwn);
				} catch (ExceptionUnmatchedClosingParentheses e) {
					errorDialog(e);
				} catch (ExceptionFileFormatOrSyntax e) {
					errorDialog(e);
				}
			} else {
				emitWarning("ERROR : The super-structure is NULL.");
			}

			switch (_RNA.get_drawMode()) {
			case RNA.DRAW_MODE_RADIATE:
				_RNA.drawRNARadiate(_conf);
				break;
			case RNA.DRAW_MODE_CIRCULAR:
				_RNA.drawRNACircle(_conf);
				break;
			case RNA.DRAW_MODE_LINEAR:
				_RNA.drawRNALine(_conf);
				break;
			case RNA.DRAW_MODE_NAVIEW:
				try {
					_RNA.drawRNANAView(_conf);
				} catch (ExceptionNAViewAlgorithm e) {
					errorDialog(e);
				}
				break;
			default:
				break;
			}

		}
	}

	/**
	 * Returns the currently selected base index, obtained through a mouse-left
	 * click
	 * 
	 * @return Selected base
	 * 
	 *         public int getSelectedBaseIndex() { return _selectedBase; }
	 * 
	 *         /** Returns the currently selected base, obtained through a
	 *         mouse-left click
	 * 
	 * @return Selected base
	 * 
	 *         public ModeleBase getSelectedBase() { return
	 *         _RNA.get_listeBases().get(_selectedBase); }
	 * 
	 *         /** Sets the selected base index
	 * 
	 * @param base
	 *            New selected base index
	 * 
	 *            public void setSelectedBase(int base) { _selectedBase = base;
	 *            }
	 */

	/**
	 * Returns the coordinates of the currently displayed RNA
	 * 
	 * @return Coordinates array
	 */
	public Point2D.Double[] getRealCoords() {
		return _realCoords;
	}

	/**
	 * Sets the coordinates of the currently displayed RNA
	 * 
	 * @param coords
	 *            New coordinates
	 */
	public void setRealCoords(Point2D.Double[] coords) {
		_realCoords = coords;
	}

	/**
	 * Returns the popup menu used for user mouse iteractions
	 * 
	 * @return Popup menu
	 */
	public VueMenu getPopup() {
		return _popup;
	}

	/**
	 * Sets the color used to display hydrogen bonds (base pairings)
	 * 
	 * @param bond_color
	 *            The color of hydrogen bonds
	 */
	public void setBondColor(Color bond_color) {
		_conf._bondColor = bond_color;
	}

	/**
	 * Returns the color used to draw the title
	 * 
	 * @return The color used to draw the title
	 */
	public Color getTitleColor() {
		return _conf._titleColor;
	}

	/**
	 * Sets the color used to draw the title
	 * 
	 * @param title_color
	 *            The new color used to draw the title
	 */
	public void setTitleColor(Color title_color) {
		_conf._titleColor = title_color;
	}

	/**
	 * Returns the height taken by the title
	 * 
	 * @return The height taken by the title
	 */
	private int getTitleHeight() {
		return _titleHeight;
	}

	/**
	 * Sets the height taken by the title
	 * 
	 * @param title_height
	 *            The height taken by the title
	 */
	@SuppressWarnings("unused")
	private void setTitleHeight(int title_height) {
		_titleHeight = title_height;
	}

	/**
	 * Returns the current state of auto centering mode.
	 * 
	 * @return True if autocentered, false otherwise
	 */
	public boolean isAutoCentered() {
		return _conf._autoCenter;
	}

	/**
	 * Sets the current state of auto centering mode.
	 * 
	 * @param center
	 *            New auto-centered state
	 */
	public void setAutoCenter(boolean center) {
		_conf._autoCenter = center;
	}

	/**
	 * Returns the font currently used for rendering the title.
	 * 
	 * @return Current title font
	 */
	public Font getTitleFont() {
		return _conf._titleFont;
	}

	/**
	 * Sets the font used for rendering the title.
	 * 
	 * @param font
	 *            New title font
	 */
	public void setTitleFont(Font font) {
		_conf._titleFont = font;
		updateTitleHeight();
	}

	/**
	 * For the LINE_MODE drawing algorithm, sets the base pair height increment,
	 * i.e. the vertical distance between two nested arcs.
	 * 
	 * @return The current base pair increment
	 */
	public double getBPHeightIncrement() {
		return _RNA._bpHeightIncrement;
	}

	/**
	 * Sets the base pair height increment, i.e. the vertical distance between
	 * two arcs to be used in LINE_MODE.
	 * 
	 * @param inc
	 *            New height increment
	 */
	public void setBPHeightIncrement(double inc) {
		_RNA._bpHeightIncrement = inc;
	}

	/**
	 * Returns the shifting of the origin of the Panel in zoom mode
	 * 
	 * @return The logical coordinate of the top-left panel point
	 */
	public Point2D.Double getOffsetPanel() {
		return _offsetPanel;
	}

	/**
	 * Returns the vector bringing the logical coordinate of left-top-most point
	 * in the panel to the left-top-most point of the RNA.
	 * 
	 * @return The logical coordinate of the top-left panel point
	 */
	private Point2D.Double getRNAOffset() {
		return _offsetRNA;
	}

	/**
	 * Returns this panel's UI menu
	 * 
	 * @return Applet's UI popupmenu
	 */
	public VueMenu getPopupMenu() {
		return _popup;
	}

	/**
	 * Returns the atomic zoom factor step, or increment.
	 * 
	 * @return Atomic zoom factor increment
	 */
	public double getZoomIncrement() {
		return _conf._zoomAmount;
	}

	/**
	 * Sets the atomic zoom factor step, or increment.
	 * 
	 * @param amount
	 *            Atomic zoom factor increment
	 */
	public void setZoomIncrement(Object amount) {
		setZoomIncrement(Float.valueOf(amount.toString()));
	}

	/**
	 * Sets the atomic zoom factor step, or increment.
	 * 
	 * @param amount
	 *            Atomic zoom factor increment
	 */
	public void setZoomIncrement(double amount) {
		_conf._zoomAmount = amount;
	}

	/**
	 * Returns the current zoom factor
	 * 
	 * @return Current zoom factor
	 */
	public double getZoom() {
		return _conf._zoom;
	}

	/**
	 * Sets the current zoom factor
	 * 
	 * @param _zoom
	 *            New zoom factor
	 */
	public void setZoom(Object _zoom) {
		double d = Float.valueOf(_zoom.toString());
		if (_conf._zoom != d) {
			_conf._zoom = d;
			fireZoomLevelChanged(d);
		}
	}

	/**
	 * Returns the translation used for zooming in and out
	 * 
	 * @return A vector describing the translation
	 */
	public Point getTranslation() {
		return _translation;
	}

	/**
	 * Sets the translation used for zooming in and out
	 * 
	 * @param trans
	 *            A vector describing the new translation
	 */
	public void setTranslation(Point trans) {
		_translation = trans;
		checkTranslation();
		fireTranslationChanged();
	}

	/**
	 * Returns the current RNA model
	 * 
	 * @return Current RNA model
	 */
	public RNA getRNA() {
		return _RNA;
	}

	/**
	 * Checks whether the drawn RNA is too large to be displayed, allowing for
	 * shifting mouse interactions.
	 * 
	 * @return true if the RNA is too large to be displayed, false otherwise
	 */
	public boolean isOutOfFrame() {
		return _horsCadre;
	}

	/**
	 * Pops up an error Dialog displaying an exception in an human-readable way.
	 * 
	 * @param error
	 *            The exception to display within the Dialog
	 */
	public void errorDialog(Exception error) {
		errorDialog(error, this);
	}

	/**
	 * Pops up an error Dialog displaying an exception in an human-readable way
	 * if errors are set to be displayed.
	 * 
	 * @see #setErrorsOn(boolean)
	 * @param error
	 *            The exception to display within the Dialog
	 * @param c
	 *            Parent component for the dialog box
	 */
	public void errorDialog(Exception error, Component c) {
		if (isErrorsOn()) {
			JOptionPane.showMessageDialog(c, error.getMessage(), "VARNA Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Pops up an error Dialog displaying an exception in an human-readable way.
	 * 
	 * @param error
	 *            The exception to display within the Dialog
	 * @param c
	 *            Parent component for the dialog box
	 */
	public static void errorDialogStatic(Exception error, Component c) {
		if (c != null) {
			JOptionPane.showMessageDialog(c, error.getMessage(),
					"VARNA Critical Error", JOptionPane.ERROR_MESSAGE);
		} else {
			System.err.println("Error: " + error.getMessage());
		}
	}

	/**
	 * Displays a warning message through a modal dialog if warnings are set to
	 * be displayed.
	 * 
	 * @see #setShowWarnings(boolean)
	 * @param warning
	 *            A message expliciting the warning
	 */
	public void emitWarning(String warning) {
		if (_conf._showWarnings)
			JOptionPane.showMessageDialog(this, warning, "VARNA Warning",
					JOptionPane.WARNING_MESSAGE);
	}

	public static void emitWarningStatic(Exception e, Component c) {
		emitWarningStatic(e.getMessage(), c);
	}

	public static void emitWarningStatic(String warning, Component c) {
		if (c != null) {
			JOptionPane.showMessageDialog(c, warning, "VARNA Warning",
					JOptionPane.WARNING_MESSAGE);
		} else {
			System.err.println("Error: " + warning);
		}
	}

	/**
	 * Toggles modifications on and off
	 * 
	 * @param modifiable
	 *            Modification status
	 */
	public void setModifiable(boolean modifiable) {
		_conf._modifiable = modifiable;
	}

	/**
	 * Returns current modification status
	 * 
	 * @return current modification status
	 */
	public boolean isModifiable() {
		return _conf._modifiable;
	}

	/**
	 * Resets the visual aspects (Zoom factor, shift) for the Panel.
	 */
	public void reset() {
		this.setBorderSize(new Dimension(0, 0));
		this.setTranslation(new Point(0, (int) (-getTitleHeight() / 2.0)));
		this.setZoom(VARNAConfig.DEFAULT_ZOOM);
		this.setZoomIncrement(VARNAConfig.DEFAULT_AMOUNT);
	}

	/**
	 * Returns the color used to draw non-standard bases
	 * 
	 * @return The color used to draw non-standard bases
	 */
	public Color getNonStandardBasesColor() {
		return _conf._specialBasesColor;
	}

	/**
	 * Sets the color used to draw non-standard bases
	 * 
	 * @param basesColor
	 *            The color used to draw non-standard bases
	 */
	public void setNonStandardBasesColor(Color basesColor) {
		_conf._specialBasesColor = basesColor;
	}

	/**
	 * Checks if the current translation doesn't "kick" the whole RNA out of the
	 * panel, and corrects the situation if necessary.
	 */
	public void checkTranslation() {
		// verification pour un zoom < 1
		if (this.getZoom() <= 1) {
			// verification sortie gauche
			if (this.getTranslation().x < -(int) ((this.getWidth() - this
					.getInnerWidth()) / 2.0)) {
				this.setTranslation(new Point(-(int) ((this.getWidth() - this
						.getInnerWidth()) / 2.0), this.getTranslation().y));
			}
			// verification sortie droite
			if (this.getTranslation().x > (int) ((this.getWidth() - this
					.getInnerWidth()) / 2.0)) {
				this.setTranslation(new Point((int) ((this.getWidth() - this
						.getInnerWidth()) / 2.0), this.getTranslation().y));
			}
			// verification sortie bas
			if (this.getTranslation().y > (int) ((this.getHeight()
					- getTitleHeight() * 2 - this.getInnerHeight()) / 2.0)) {
				this.setTranslation(new Point(this.getTranslation().x,
						(int) ((this.getHeight() - getTitleHeight() * 2 - this
								.getInnerHeight()) / 2.0)));
			}
			// verification sortie haut
			if (this.getTranslation().y < -(int) ((this.getHeight() - this
					.getInnerHeight()) / 2.0)) {
				this.setTranslation(new Point(
						this.getTranslation().x,
						-(int) ((this.getHeight() - this.getInnerHeight()) / 2.0)));
			}
		} else {
			// zoom > 1
			Rectangle r2 = getZoomedInTranslationBox();
			int LBoundX = r2.x;
			int UBoundX = r2.x + r2.width;
			int LBoundY = r2.y;
			int UBoundY = r2.y + r2.height;
			if (this.getTranslation().x < LBoundX) {
				this.setTranslation(new Point(LBoundX, getTranslation().y));
			} else if (this.getTranslation().x > UBoundX) {
				this.setTranslation(new Point(UBoundX, getTranslation().y));
			}
			if (this.getTranslation().y < LBoundY) {
				this.setTranslation(new Point(getTranslation().x, LBoundY));
			} else if (this.getTranslation().y > UBoundY) {
				this.setTranslation(new Point(getTranslation().x, UBoundY));
			}
		}
	}

	public Rectangle getZoomedInTranslationBox() {
		int LBoundX = -(int) ((this.getInnerWidth()) / 2.0);
		int UBoundX = (int) ((this.getInnerWidth()) / 2.0);
		int LBoundY = -(int) ((this.getInnerHeight()) / 2.0);
		int UBoundY = (int) ((this.getInnerHeight()) / 2.0);
		return new Rectangle(LBoundX, LBoundY, UBoundX - LBoundX, UBoundY
				- LBoundY);

	}

	/**
	 * Returns the "real pixels" x-coordinate of the RNA.
	 * 
	 * @return X-coordinate of the translation
	 */
	public int getLeftOffset() {
		return _border.width
				+ ((this.getWidth() - 2 * _border.width) - this.getInnerWidth())
				/ 2 + _translation.x;
	}

	/**
	 * Returns the "real pixels" width of the drawing surface for our RNA.
	 * 
	 * @return Width of the drawing surface for our RNA
	 */
	public int getInnerWidth() {
		// Largeur du dessin
		return (int) Math.round((this.getWidth() - 2 * _border.width)
				* _conf._zoom);
	}

	/**
	 * Returns the "real pixels" y-coordinate of the RNA.
	 * 
	 * @return Y-coordinate of the translation
	 */
	public int getTopOffset() {
		return _border.height
				+ ((this.getHeight() - 2 * _border.height) - this
						.getInnerHeight()) / 2 + _translation.y;
	}

	/**
	 * Returns the "real pixels" height of the drawing surface for our RNA.
	 * 
	 * @return Height of the drawing surface for our RNA
	 */
	public int getInnerHeight() {
		// Hauteur du dessin
		return (int) Math.round((this.getHeight()) * _conf._zoom - 2
				* _border.height - getTitleHeight());
	}

	/**
	 * Checks if the current mode is the "comparison" mode
	 * 
	 * @return True if comparison, false otherwise
	 */
	public boolean isComparisonMode() {
		return _conf._comparisonMode;
	}

	/**
	 * Rotates the RNA coordinates by a certain angle
	 * 
	 * @param angleDegres
	 *            Rotation angle, in degrees
	 */
	public void globalRotation(Double angleDegres) {
		_RNA.globalRotation(angleDegres);
		fireLayoutChanged();
		repaint();
	}

	/**
	 * Returns the index of the currently selected base, defaulting to the
	 * closest base to the last mouse-click.
	 * 
	 * @return Index of the currently selected base
	 */
	public Integer getNearestBase() {
		return _nearestBase;
	}

	/**
	 * Sets the index of the currently selected base.
	 * 
	 * @param base
	 *            Index of the new selected base
	 */
	public void setNearestBase(Integer base) {
		_nearestBase = base;
	}

	/**
	 * Returns the color used to draw 'Gaps' bases in comparison mode
	 * 
	 * @return Color used for 'Gaps'
	 */
	public Color getGapsBasesColor() {
		return _conf._dashBasesColor;
	}

	/**
	 * Sets the color to use for 'Gaps' bases in comparison mode
	 * 
	 * @param c
	 *            Color used for 'Gaps'
	 */
	public void setGapsBasesColor(Color c) {
		_conf._dashBasesColor = c;
	}

	@SuppressWarnings("unused")
	private void imprimer() {
		// PrintPanel canvas;
		// canvas = new PrintPanel();
		PrintRequestAttributeSet attributes;
		attributes = new HashPrintRequestAttributeSet();
		try {
			PrinterJob job = PrinterJob.getPrinterJob();
			// job.setPrintable(this);
			if (job.printDialog(attributes)) {
				job.print(attributes);
			}
		} catch (PrinterException exception) {
			errorDialog(exception);
		}
	}

	/**
	 * Checks whether errors are to be displayed
	 * 
	 * @return Error display status
	 */
	public boolean isErrorsOn() {
		return _conf._errorsOn;
	}

	/**
	 * Sets whether errors are to be displayed
	 * 
	 * @param on
	 *            New error display status
	 */
	public void setErrorsOn(boolean on) {
		_conf._errorsOn = on;
	}

	/**
	 * Returns the view associated with user interactions
	 * 
	 * @return A view associated with user interactions
	 */
	public VueUI getVARNAUI() {
		return _UI;
	}

	/**
	 * Toggles on/off using base inner color for drawing base-pairs
	 * 
	 * @param on
	 *            True for using base inner color for drawing base-pairs, false
	 *            for classic mode
	 */
	public void setUseBaseColorsForBPs(boolean on) {
		_conf._useBaseColorsForBPs = on;
	}

	/**
	 * Returns true if current base color is used as inner color for drawing
	 * base-pairs
	 * 
	 * @return True for using base inner color for drawing base-pairs, false for
	 *         classic mode
	 */
	public boolean getUseBaseColorsForBPs() {
		return _conf._useBaseColorsForBPs;
	}

	/**
	 * Toggles on/off using a special color used for drawing "non-standard"
	 * bases
	 * 
	 * @param on
	 *            True for using a special color used for drawing "non-standard"
	 *            bases, false for classic mode
	 */
	public void setColorNonStandardBases(boolean on) {
		_conf._colorSpecialBases = on;
	}

	/**
	 * Returns true if a special color is used as inner color for non-standard
	 * base
	 * 
	 * @return True for using a special color used for drawing "non-standard"
	 *         bases, false for classic mode
	 */
	public boolean getColorSpecialBases() {
		return _conf._colorSpecialBases;
	}

	/**
	 * Toggles on/off using a special color used for drawing "Gaps" bases in
	 * comparison mode
	 * 
	 * @param on
	 *            True for using a special color used for drawing "Gaps" bases
	 *            in comparison mode, false for classic mode
	 */
	public void setColorGapsBases(boolean on) {
		_conf._colorDashBases = on;
	}

	/**
	 * Returns true if a special color is used for drawing "Gaps" bases in
	 * comparison mode
	 * 
	 * @return True for using a special color used for drawing "Gaps" bases in
	 *         comparison mode, false for classic mode
	 */
	public boolean getColorGapsBases() {
		return _conf._colorDashBases;
	}

	/**
	 * Toggles on/off displaying warnings
	 * 
	 * @param on
	 *            True to display warnings, false otherwise
	 */
	public void setShowWarnings(boolean on) {
		_conf._showWarnings = on;
	}

	/**
	 * Get current warning display status
	 * 
	 * @return True to display warnings, false otherwise
	 */
	public boolean getShowWarnings() {
		return _conf._showWarnings;
	}

	/**
	 * Toggles on/off displaying non-canonical base-pairs
	 * 
	 * @param on
	 *            True to display NC base-pairs, false otherwise
	 */
	public void setShowNonCanonicalBP(boolean on) {
		_conf._drawnNonCanonicalBP = on;
	}

	/**
	 * Return the current display status for non-canonical base-pairs
	 * 
	 * @return True if NC base-pairs are displayed, false otherwise
	 */
	public boolean getShowNonCanonicalBP() {
		return _conf._drawnNonCanonicalBP;
	}

	/**
	 * Toggles on/off displaying "non-planar" base-pairs
	 * 
	 * @param on
	 *            True to display "non-planar" base-pairs, false otherwise
	 */
	public void setShowNonPlanarBP(boolean on) {
		_conf._drawnNonPlanarBP = on;
	}

	/**
	 * Return the current display status for non-planar base-pairs
	 * 
	 * @return True if non-planars base-pairs are displayed, false otherwise
	 */
	public boolean getShowNonPlanarBP() {
		return _conf._drawnNonPlanarBP;
	}

	/**
	 * Sets the base-pair representation style
	 * 
	 * @param st
	 *            The new base-pair style
	 */
	public void setBPStyle(VARNAConfig.BP_STYLE st) {
		_conf._mainBPStyle = st;
	}

	/**
	 * Returns the base-pair representation style
	 * 
	 * @return The current base-pair style
	 */
	public VARNAConfig.BP_STYLE getBPStyle() {
		return _conf._mainBPStyle;
	}

	/**
	 * Returns the current VARNA Panel configuration. The returned instance
	 * should not be modified directly, but rather through the getters/setters
	 * from the VARNAPanel class.
	 * 
	 * @return Current configuration
	 */
	public VARNAConfig getConfig() {
		return _conf;
	}

	/**
	 * Sets the background color
	 * 
	 * @param c
	 *            New background color
	 */
	@Override
  public void setBackground(Color c) {
		if (_conf != null) {
			if (c != null) {
				_conf._backgroundColor = c;
				_conf._drawBackground = (!c
						.equals(VARNAConfig.DEFAULT_BACKGROUND_COLOR));
			} else {
				_conf._backgroundColor = VARNAConfig.DEFAULT_BACKGROUND_COLOR;
				_conf._drawBackground = false;
			}
		}

	}

	/**
	 * Starts highlighting the selected base.
	 */
	public void highlightSelectedBase(ModeleBase m) {
		ArrayList<Integer> v = new ArrayList<Integer>();
		int sel = m.getIndex();
		if (sel != -1) {
			v.add(sel);
		}
		setSelection(v);
	}

	/**
	 * Starts highlighting the selected base.
	 */
	public void highlightSelectedStem(ModeleBase m) {
		ArrayList<Integer> v = new ArrayList<Integer>();
		int sel = m.getIndex();
		if (sel != -1) {
			ArrayList<Integer> r = _RNA.findStem(sel);
			v.addAll(r);
		}
		setSelection(v);
	}

	public BaseList getSelection() {
		return _selectedBases;
	}

	public ArrayList<Integer> getSelectionIndices() {
		return _selectedBases.getIndices();
	}

	public void setSelection(ArrayList<Integer> indices) {
		setSelection(_RNA.getBasesAt(indices));
	}

	public void setSelection(Collection<? extends ModeleBase> mbs) {
		BaseList bck = new BaseList(_selectedBases);
		_selectedBases.clear();
		_selectedBases.addBases(mbs);
		_blink.setActive(true);
		fireSelectionChanged(bck, _selectedBases);
	}

	public ArrayList<Integer> getBasesInRectangleDiff(Rectangle recIn,
			Rectangle recOut) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < _realCoords.length; i++) {
			if (recIn.contains(_realCoords[i])
					^ recOut.contains(_realCoords[i]))
				result.add(i);
		}
		return result;
	}

	public ArrayList<Integer> getBasesInRectangle(Rectangle rec) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < _realCoords.length; i++) {
			if (rec.contains(_realCoords[i]))
				result.add(i);
		}
		return result;
	}

	public void setSelectionRectangle(Rectangle rec) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (_selectionRectangle != null) {
			result = getBasesInRectangleDiff(_selectionRectangle, rec);
		} else {
			result = getBasesInRectangle(rec);
		}
		_selectionRectangle = new Rectangle(rec);
		toggleSelection(result);
		repaint();
	}

	public void removeSelectionRectangle() {
		_selectionRectangle = null;
	}

	public void addToSelection(Collection<? extends Integer> indices) {
		for (int i : indices) {
			addToSelection(i);
		}
	}

	public void addToSelection(int i) {
		BaseList bck = new BaseList(_selectedBases);
		ModeleBase mb = _RNA.getBaseAt(i);
		_selectedBases.addBase(mb);
		_blink.setActive(true);
		fireSelectionChanged(bck, _selectedBases);
	}

	public void removeFromSelection(int i) {
		BaseList bck = new BaseList(_selectedBases);
		ModeleBase mb = _RNA.getBaseAt(i);
		_selectedBases.removeBase(mb);
		if (_selectedBases.size() == 0) {
			_blink.setActive(false);
		} else {
			_blink.setActive(true);
		}
		fireSelectionChanged(bck, _selectedBases);
	}

	public boolean isInSelection(int i) {
		return _selectedBases.contains(_RNA.getBaseAt(i));
	}

	public void toggleSelection(int i) {
		if (isInSelection(i))
			removeFromSelection(i);
		else
			addToSelection(i);
	}

	public void toggleSelection(Collection<? extends Integer> indices) {
		for (int i : indices) {
			toggleSelection(i);
		}
	}

	/**
	 * Stops highlighting bases
	 */
	public void clearSelection() {
		BaseList bck = new BaseList(_selectedBases);
		_selectedBases.clear();
		_blink.setActive(false);
		repaint();
		fireSelectionChanged(bck, _selectedBases);
	}

	public void saveSelection() {
		_backupSelection.clear();
		_backupSelection.addAll(_selectedBases.getBases());
	}

	public void restoreSelection() {
		setSelection(_backupSelection);
	}

	/**
	 * Stops highlighting bases
	 */
	public void resetAnnotationHighlight() {
		_highlightAnnotation = false;
		repaint();
	}

	/**
	 * Toggles on/off a rectangular outline of the bounding box.
	 * 
	 * @param on
	 *            True to draw the bounding box, false otherwise
	 */
	public void drawBBox(boolean on) {
		_drawBBox = on;
	}

	/**
	 * Toggles on/off a rectangular outline of the border.
	 * 
	 * @param on
	 *            True to draw the bounding box, false otherwise
	 */
	public void drawBorder(boolean on) {
		_drawBorder = on;
	}

	public void setBaseInnerColor(Color c) {
		_RNA.setBaseInnerColor(c);
	}

	public void setBaseNumbersColor(Color c) {
		_RNA.setBaseNumbersColor(c);
	}

	public void setBaseNameColor(Color c) {
		_RNA.setBaseNameColor(c);
	}

	public void setBaseOutlineColor(Color c) {
		_RNA.setBaseOutlineColor(c);
	}

	public ArrayList<TextAnnotation> getListeAnnotations() {
		return _RNA.getAnnotations();
	}

	public void resetListeAnnotations() {
		_RNA.clearAnnotations();
		repaint();
	}

	public void addAnnotation(TextAnnotation textAnnotation) {
		_RNA.addAnnotation(textAnnotation);
		repaint();
	}

	public boolean removeAnnotation(TextAnnotation textAnnotation) {
		boolean done = _RNA.removeAnnotation(textAnnotation);
		repaint();
		return done;
	}

	public TextAnnotation get_selectedAnnotation() {
		return _selectedAnnotation;
	}

	public void set_selectedAnnotation(TextAnnotation annotation) {
		_selectedAnnotation = annotation;
	}

	public void removeSelectedAnnotation() {
		_highlightAnnotation = false;
		_selectedAnnotation = null;
	}

	public void highlightSelectedAnnotation() {
		_highlightAnnotation = true;
	}

	public boolean getFlatExteriorLoop() {
		return _conf._flatExteriorLoop;
	}

	public void setFlatExteriorLoop(boolean on) {
		_conf._flatExteriorLoop = on;
	}

	public void setLastSelectedPosition(Point2D.Double p) {
		_lastSelectedCoord.x = p.x;
		_lastSelectedCoord.y = p.y;
	}

	public Point2D.Double getLastSelectedPosition() {
		return _lastSelectedCoord;
	}

	public void setSequence(String s) {
		_RNA.setSequence(s);
		repaint();
	}

	public void setColorMapVisible(boolean b) {
		_conf._drawColorMap = b;
		repaint();
	}

	public boolean getColorMapVisible() {
		return _conf._drawColorMap;
	}

	public void removeColorMap() {
		_conf._drawColorMap = false;
		repaint();
	}

	public void saveSession(String path) {
		/*
		 * FileOutputStream fos = null; ObjectOutputStream out = null; try { fos
		 * = new FileOutputStream(path); out = new ObjectOutputStream(fos);
		 * out.writeObject(new FullBackup(_conf, _RNA, _conf._title));
		 * out.close(); } catch (Exception ex) { ex.printStackTrace(); }
		 */
		toXML(path);
	}

	public FullBackup loadSession(File path) throws ExceptionLoadingFailed {

		FullBackup bck = importSession(path);
		Mapping map = Mapping.DefaultOutermostMapping(getRNA().getSize(),
				bck.rna.getSize());
		showRNAInterpolated(bck.rna, map);
		_conf = bck.config;
		repaint();
		return bck;
	}

	public static String VARNA_SESSION_EXTENSION = "varna";

	public static FullBackup importSession(Object path) // BH was String
			throws ExceptionLoadingFailed {
		try {
			FileInputStream fis = (path instanceof File ? new FileInputStream((File) path) : new FileInputStream(path.toString()));
			// ZipInputStream zis = new
			// ZipInputStream(new BufferedInputStream(fis));
			// zis.getNextEntry();
			FullBackup h = importSession(fis, path.toString());
			// zis.close();
			return h;
		} catch (FileNotFoundException e) {
			throw (new ExceptionLoadingFailed("File not found.", path.toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw (new ExceptionLoadingFailed(
					"I/O error while loading session.", path.toString()));
		}
	}

	public static FullBackup importSession(InputStream fis, String path)
			throws ExceptionLoadingFailed {
		System.setProperty("javax.xml.parsers.SAXParserFactory",
				"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
		SAXParserFactory saxFact = javax.xml.parsers.SAXParserFactory
				.newInstance();
		saxFact.setValidating(false);
		saxFact.setXIncludeAware(false);
		saxFact.setNamespaceAware(false);
		try {
			SAXParser sp = saxFact.newSAXParser();
			VARNASessionParser sessionData = new VARNASessionParser();
			sp.parse(fis, sessionData);
			FullBackup res = new FullBackup(sessionData.getVARNAConfig(),
					sessionData.getRNA(), "test"); 
			return res;
		} catch (ParserConfigurationException e) {
			throw new ExceptionLoadingFailed("Bad XML parser configuration",
					path);
		} catch (SAXException e) {
			throw new ExceptionLoadingFailed("XML parser Exception", path);
		} catch (IOException e) {
			throw new ExceptionLoadingFailed("I/O error", path);
		}
	}

	public void loadFile(File path) {
		loadFile(path, false);
	}

	public boolean getDrawBackbone() {
		return _conf._drawBackbone;
	}

	public void setDrawBackbone(boolean b) {
		_conf._drawBackbone = b;
	}

	public void addHighlightRegion(HighlightRegionAnnotation n) {
		_RNA.addHighlightRegion(n);
	}

	public void removeHighlightRegion(HighlightRegionAnnotation n) {
		_RNA.removeHighlightRegion(n);
	}

	public void addHighlightRegion(int i, int j) {
		_RNA.addHighlightRegion(i, j);
	}

	public void addHighlightRegion(int i, int j, Color fill, Color outline,
			double radius) {
		_RNA.addHighlightRegion(i, j, fill, outline, radius);
	}
	
	public void loadRNA(String path) {
		loadRNA(path, false);
	}
	
	public void loadRNA(Object path, boolean interpolate) { // BH was String
		try {
			Collection<RNA> rnas = (path instanceof File ? RNAFactory.loadSecStr(new FileReader((File) path)) : RNAFactory.loadSecStr(path.toString()));
			if (rnas.isEmpty()) {
				throw new ExceptionFileFormatOrSyntax(
						"No RNA could be parsed from that source.");
			}
			RNA rna = rnas.iterator().next();
			try {
				rna.drawRNA(_conf);
			} catch (ExceptionNAViewAlgorithm e) {
				e.printStackTrace();
			}
			if (!interpolate) {
				showRNA(rna);
			} else {
				this.showRNAInterpolated(rna);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ExceptionFileFormatOrSyntax e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadFile(File path, boolean interpolate) { // was String BH StringJS
		try {
			loadSession(path);
		} catch (Exception e1) {
			loadRNA(path, interpolate);
		}
	}

	public void setConfig(VARNAConfig cfg) {
		_conf = cfg;
	}

	public void toggleDrawOutlineBases() {
		_conf._drawOutlineBases = !_conf._drawOutlineBases;
	}

	public void toggleFillBases() {
		_conf._fillBases = !_conf._fillBases;
	}

	public void setDrawOutlineBases(boolean drawn) {
		_conf._drawOutlineBases = drawn;
	}

	public void setFillBases(boolean drawn) {
		_conf._fillBases = drawn;
	}

	public void readValues(Reader r) {
		this._RNA.readValues(r, _conf._cm);
	}

	public void addVARNAListener(InterfaceVARNAListener v) {
		_VARNAListeners.add(v);
	}

	public void fireLayoutChanged() {
		for (InterfaceVARNAListener v : _VARNAListeners) {
			v.onStructureRedrawn();
		}
	}

	public void fireUINewStructure(RNA r) {
		for (InterfaceVARNAListener v : _VARNAListeners) {
			v.onUINewStructure(_conf, r);
		}
	}

	public void fireZoomLevelChanged(double d) {
		for (InterfaceVARNAListener v : _VARNAListeners) {
			v.onZoomLevelChanged();
		}
	}

	public void fireTranslationChanged() {
		for (InterfaceVARNAListener v2 : _VARNAListeners) {
			v2.onTranslationChanged();
		}
	}

	public void addSelectionListener(InterfaceVARNASelectionListener v) {
		_selectionListeners.add(v);
	}

	public void fireSelectionChanged(BaseList mold, BaseList mnew) {
		BaseList addedBases = mnew.removeAll(mold);
		BaseList removedBases = mold.removeAll(mnew);
		for (InterfaceVARNASelectionListener v2 : _selectionListeners) {
			v2.onSelectionChanged(mnew, addedBases, removedBases);
		}
	}

	public void fireHoverChanged(ModeleBase mold, ModeleBase mnew) {
		for (InterfaceVARNASelectionListener v2 : _selectionListeners) {
			v2.onHoverChanged(mold, mnew);
		}
	}

	public void addRNAListener(InterfaceVARNARNAListener v) {
		_RNAListeners.add(v);
	}

	public void addVARNABasesListener(InterfaceVARNABasesListener l) {
		_basesListeners.add(l);
	}

	public void fireSequenceChanged(int index, String oldseq, String newseq) {
		for (InterfaceVARNARNAListener v2 : _RNAListeners) {
			v2.onSequenceModified(index, oldseq, newseq);
		}
	}

	public void fireStructureChanged(Set<ModeleBP> current,
			Set<ModeleBP> addedBasePairs, Set<ModeleBP> removedBasePairs) {
		for (InterfaceVARNARNAListener v2 : _RNAListeners) {
			v2.onStructureModified(current, addedBasePairs, removedBasePairs);
		}
	}

	public void fireLayoutChanged(
			Hashtable<Integer, Point2D.Double> movedPositions) {
		for (InterfaceVARNARNAListener v2 : _RNAListeners) {
			v2.onRNALayoutChanged(movedPositions);
		}
	}

	public void fireBaseClicked(ModeleBase mb, MouseEvent me) {
		if (mb != null) {
			for (InterfaceVARNABasesListener v2 : _basesListeners) {
				v2.onBaseClicked(mb, me);
			}
		}
	}

	public double getOrientation() {
		return _RNA.getOrientation();
	}

	public ModeleBase _hoveredBase = null;

	public void setHoverBase(ModeleBase m) {
		if (m != _hoveredBase) {
			ModeleBase bck = _hoveredBase;
			_hoveredBase = m;
			repaint();
			fireHoverChanged(bck, m);
		}
	}

	public void toXML(String path) {
		FileOutputStream fis;
		try {
			fis = new FileOutputStream(path);
			// ZipOutputStream zis = new ZipOutputStream(new
			// BufferedOutputStream(fis));
			// ZipEntry entry = new ZipEntry("VARNASession");
			// zis.putNextEntry(entry);
			PrintWriter pw = new PrintWriter(fis);
			toXML(pw);
			pw.flush();
			// zis.closeEntry();
			// zis.close();
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void toXML(PrintWriter out) {
		try {

			// out = new PrintWriter(System.out);
			StreamResult streamResult = new StreamResult(out);
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
					.newInstance();
			// SAX2.0 ContentHandler.
			TransformerHandler hd = tf.newTransformerHandler();
			Transformer serializer = hd.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
			serializer
					.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "users.dtd");
			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			hd.setResult(streamResult);
			hd.startDocument();
			toXML(hd);
			hd.endDocument();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static String XML_ELEMENT_NAME = "VARNASession";

	public void toXML(TransformerHandler hd) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("", "", XML_ELEMENT_NAME, atts);
		_RNA.toXML(hd);
		_conf.toXML(hd);
		hd.endElement("", "", XML_ELEMENT_NAME);
	}

	public TextAnnotation getNearestAnnotation(int x, int y) {
		TextAnnotation t = null;
		if (getListeAnnotations().size() != 0) {
			double dist = Double.MAX_VALUE;
			double d2;
			Point2D.Double position;
			for (TextAnnotation textAnnot : getListeAnnotations()) {
				// calcul de la distance
				position = textAnnot.getCenterPosition();
				position = transformCoord(position);
				d2 = Math.sqrt(Math.pow((position.x - x), 2)
						+ Math.pow((position.y - y), 2));
				// si la valeur est inferieur au minimum actuel
				if ((dist > d2)
						&& (d2 < getScaleFactor()
								* ControleurClicMovement.MIN_SELECTION_DISTANCE)) {
					t = textAnnot;
					dist = d2;
				}
			}
		}
		return t;
	}

	public ModeleBase getNearestBase(int x, int y, boolean always,
			boolean onlyPaired) {
		int i = getNearestBaseIndex(x, y, always, onlyPaired);
		if (i == -1)
			return null;
		return getRNA().get_listeBases().get(i);
	}

	public ModeleBase getNearestBase(int x, int y) {
		return getNearestBase(x, y, false, false);
	}

	public int getNearestBaseIndex(int x, int y, boolean always,
			boolean onlyPaired) {
		double d2, dist = Double.MAX_VALUE;
		int mb = -1;
		for (int i = 0; i < getRealCoords().length; i++) {
			if (!onlyPaired
					|| (getRNA().get_listeBases().get(i).getElementStructure() != -1)) {
				d2 = Math.sqrt(Math.pow((getRealCoords()[i].x - x), 2)
						+ Math.pow((getRealCoords()[i].y - y), 2));
				if ((dist > d2)
						&& ((d2 < getScaleFactor()
								* ControleurClicMovement.MIN_SELECTION_DISTANCE) || always)) {
					dist = d2;
					mb = i;
				}
			}
		}
		return mb;
	}

	public void globalRescale(double factor) {
		_RNA.rescale(factor);
		fireLayoutChanged();
		repaint();
	}

	public void setSpaceBetweenBases(double sp) {
		_conf._spaceBetweenBases = sp;
	}

	public double getSpaceBetweenBases() {
		return _conf._spaceBetweenBases;
	}


}
