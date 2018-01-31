package org.jmol.adapter.readers.pymol;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.P3;

import org.jmol.util.Logger;

/**
 * PyMOL settings and constants. 
 * 
 *  see http://sourceforge.net/projects/pymol/files/pymol/
 *  
 *  Settings: http://sourceforge.net/p/pymol/code/4025/tree/trunk/pymol/layer1/Setting.h
 *
 *  Colors: http://sourceforge.net/p/pymol/code/4008/tree/trunk/pymol/layer1/Color.c 
 *  
 *  Colors there are created on the fly. The colors here were derived from Color.pm
 *  (Roni Gordon, via Jaime Prilusky, Jan 2013)
 *  
 *  with note therein:
 *  
 *     for i in cmd.get_color_indices(1):
 *      print>>fp, (
 *       ( i[1], i[0], ( int( cmd.get_color_tuple(i[0])[0]*255 ), 
 *                       int( cmd.get_color_tuple(i[0])[1]*255 ), 
 *                       int(cmd.get_color_tuple(i[0])[2]*255 ) 
 *                      ) 
 *       ) 
 *      );
 *
 * which I then turned into the list below using Excel.
 * 
 * I assume there are also custom colors that need to be considered; we are not doing that here yet.
 * 
 * 
 *  -- Bob Hanson, Feb 2013
 *  
 */

class PyMOL {


  final static int OBJECT_SELECTION = -1;
  final static int OBJECT_MOLECULE = 1;
  final static int OBJECT_MAPDATA = 2;
  final static int OBJECT_MAPMESH = 3;
  final static int OBJECT_MEASURE = 4;
  final static int OBJECT_CALLBACK = 5;
  final static int OBJECT_CGO = 6; // compiled graphics object
  final static int OBJECT_SURFACE = 7;
  final static int OBJECT_GADGET = 8;
  final static int OBJECT_CALCULATOR = 9;
  final static int OBJECT_SLICE = 10;
  final static int OBJECT_ALIGNMENT = 11;
  final static int OBJECT_GROUP = 12;

  final static String[] REP_LIST = new String[] { 
    "sticks","spheres","surface",
    "labels","nb_spheres",
    "cartoon","ribbon","lines",
    "mesh","dots","dashes",
    "nonbonded","cell","cgo","callback","extent",
    "slice","angles","dihedrals","ellipsoid","volume"};


  final static int REP_STICKS = 0;
  final static int REP_SPHERES = 1;
  final static int REP_SURFACE = 2; // objSurface
  final static int REP_LABELS = 3;
  final static int REP_NBSPHERES = 4;
  final static int REP_CARTOON = 5;
  final static int REP_RIBBON = 6;
  final static int REP_LINES = 7; 
  final static int REP_MESH = 8; // objMesh
  final static int REP_DOTS = 9; // dots; also used for objMap
  final static int REP_DASHES = 10;  // for measurements
  final static int REP_NONBONDED = 11;
  
  final static int REP_CELL = 12; // for objMesh, objSurface
  final static int REP_CGO = 13; // for sculpt mode, objAlignment, objCGO
  final static int REP_CALLBACK = 14; // for objCallback
  final static int REP_EXTENT = 15; // for objMap
  final static int REP_SLICE = 16; // for objSlice
  final static int REP_ANGLES = 17;
  final static int REP_DIHEDRALS = 18;
  final static int REP_ELLIPSOID = 19;
  final static int REP_VOLUME = 20;

  final static int REP_MAX = 21;

  // a continuation of PyMOL.REP_xxx
  final static int REP_JMOL_TRACE = 21;
  final static int REP_JMOL_PUTTY = 22;
  final static int REP_JMOL_MAX = 23;

  // ???
  
  // flag 24: 
  
  // a[24] - don't surface these atoms (waters, ligands, etc.) 
  static int FLAG_exfoliate     = 0x01000000;
  // a[24] - ignore atoms altogether when surfacing 
  static int  FLAG_ignore        = 0x02000000;
  // a[24] - disable cartoon smoothing for these atoms 
  static int  FLAG_no_smooth     = 0x04000000;
  // a[24] - polymer 
  static int  FLAG_polymer       = 0x08000000;
  // a[24] - waters 
  static int  FLAG_solvent       = 0x10000000;
  // a[24] - organics 
  static int  FLAG_organic       = 0x20000000;
  // a[24] - inorganics 
  static int  FLAG_inorganic     = 0x40000000;
  
  static int FLAG_NOSURFACE = FLAG_ignore | FLAG_exfoliate;
  
  // settings: There are 715 of these...
  
  final static int active_selections                     = 351;
  final static int alignment_as_cylinders                = 692;
  final static int all_states                            =  49;
  final static int ambient                               =   7;
  final static int ambient_occlusion_mode                = 702;
  final static int ambient_occlusion_scale               = 703;
  final static int ambient_occlusion_smooth              = 704;
  final static int anaglyph_mode                         = 706;
  final static int angle_color                           = 575;
  final static int angle_label_position                  = 406;
  final static int angle_size                            = 405;
  final static int animation                             = 388;
  final static int animation_duration                    = 389;
  final static int antialias                             =  12;
  final static int async_builds                          = 506;
  final static int ati_bugs                              = 585;
  final static int atom_name_wildcard                    = 413;
  final static int atom_type_format                      = 660;
  final static int auto_classify_atoms                   = 360;
  final static int auto_color                            = 238;
  final static int auto_color_next                       = 239;
  final static int auto_copy_images                      = 557;
  final static int auto_defer_atom_count                 = 653;
  final static int auto_defer_builds                     = 567;
  final static int auto_dss                              = 323;
  final static int auto_hide_selections                  =  79;
  final static int auto_indicate_flags                   = 147;
  final static int auto_number_selections                = 443;
  final static int auto_overlay                          = 603;
  final static int auto_remove_hydrogens                 = 158;
  final static int auto_rename_duplicate_objects         = 561;
  final static int auto_sculpt                           = 162;
  final static int auto_show_lines                       =  51;
  final static int auto_show_nonbonded                   =  72;
  final static int auto_show_selections                  =  78;
  final static int auto_show_spheres                     = 420;
  final static int auto_zoom                             =  60;
  final static int autoclose_dialogs                     = 661;
  final static int backface_cull                         =  75;
  final static int batch_prefix                          = 187;
  final static int bg_gradient                           = 662;
  final static int bg_image_filename                     = 712;
  final static int bg_image_mode                         = 713;
  final static int bg_image_tilesize                     = 714;
  final static int bg_image_linear                       = 715; 
  final static int bg_rgb                                =   6;
  final static int bg_rgb_bottom                         = 664;
  final static int bg_rgb_top                            = 663;
  final static int bonding_vdw_cutoff                    =   0;
  final static int button_mode                           =  63;
  final static int button_mode_name                      = 330;
  final static int cache_display                         =  73;
  final static int cache_frames                          =  31;
  final static int cache_max                             = 578;
  final static int cache_memory                          = 264;
  final static int cache_mode                            = 573;
  final static int cartoon_color                         = 236;
  final static int cartoon_cylindrical_helices           = 180;
  final static int cartoon_debug                         = 105;
  final static int cartoon_discrete_colors               = 125;
  final static int cartoon_dumbbell_length               = 115;
  final static int cartoon_dumbbell_radius               = 117;
  final static int cartoon_dumbbell_width                = 116;
  final static int cartoon_fancy_helices                 = 118;
  final static int cartoon_fancy_sheets                  = 119;
  final static int cartoon_flat_cycles                   = 260;
  final static int cartoon_flat_sheets                   = 113;
  final static int cartoon_helix_radius                  = 181;
  final static int cartoon_highlight_color               = 241;
  final static int cartoon_ladder_color                  = 450;
  final static int cartoon_ladder_mode                   = 448;
  final static int cartoon_ladder_radius                 = 449;
  final static int cartoon_loop_cap                      = 432;
  final static int cartoon_loop_quality                  =  93;
  final static int cartoon_loop_radius                   =  92;
  final static int cartoon_nucleic_acid_as_cylinders     = 693;
  final static int cartoon_nucleic_acid_color            = 451;
  final static int cartoon_nucleic_acid_mode             = 361;
  final static int cartoon_oval_length                   = 100;
  final static int cartoon_oval_quality                  = 102;
  final static int cartoon_oval_width                    = 101;
  final static int cartoon_power                         =  94;
  final static int cartoon_power_b                       =  95;
  final static int cartoon_putty_quality                 = 378;
  final static int cartoon_putty_radius                  = 377;
  final static int cartoon_putty_range                   = 382;
  final static int cartoon_putty_scale_max               = 380;
  final static int cartoon_putty_scale_min               = 379;
  final static int cartoon_putty_scale_power             = 381;
  final static int cartoon_putty_transform               = 581;
  final static int cartoon_rect_length                   =  96;
  final static int cartoon_rect_width                    =  97;
  final static int cartoon_refine                        = 123;
  final static int cartoon_refine_normals                = 112;
  final static int cartoon_refine_tips                   = 124;
  final static int cartoon_ring_color                    = 429;
  final static int cartoon_ring_finder                   = 430;
  final static int cartoon_ring_mode                     = 427;
  final static int cartoon_ring_radius                   = 508;
  final static int cartoon_ring_transparency             = 452;
  final static int cartoon_ring_width                    = 428;
  final static int cartoon_round_helices                 = 111;
  final static int cartoon_sampling                      =  91;
  final static int cartoon_side_chain_helper             = 383;
  final static int cartoon_smooth_cycles                 = 259;
  final static int cartoon_smooth_first                  = 257;
  final static int cartoon_smooth_last                   = 258;
  final static int cartoon_smooth_loops                  = 114;
  final static int cartoon_throw                         = 122;
  final static int cartoon_trace_atoms                   = 269;
  final static int cartoon_transparency                  = 279;
  final static int cartoon_tube_cap                      = 431;
  final static int cartoon_tube_quality                  = 104;
  final static int cartoon_tube_radius                   = 103;
  final static int cartoon_use_shader                    = 643;
  final static int cavity_cull                           =  13;
  final static int cgo_debug                             = 674;
  final static int cgo_dot_radius                        = 303;
  final static int cgo_dot_width                         = 302;
  final static int cgo_ellipsoid_quality                 = 564;
  final static int cgo_lighting                          = 671;
  final static int cgo_line_radius                       = 130;
  final static int cgo_line_width                        = 129;
  final static int cgo_ray_width_scale                   = 109;
  final static int cgo_shader_ub_color                   = 669;
  final static int cgo_shader_ub_flags                   = 694;
  final static int cgo_shader_ub_normal                  = 670;
  final static int cgo_sphere_quality                    = 189;
  final static int cgo_transparency                      = 441;
  final static int cgo_use_shader                        = 668;
  final static int clamp_colors                          = 214;
  final static int clean_electro_mode                    = 615;
  final static int cone_quality                          = 583;
  final static int connect_bonded                        = 487;
  final static int connect_cutoff                        = 182;
  final static int connect_mode                          = 179;
  final static int coulomb_cutoff                        = 367;
  final static int coulomb_dielectric                    = 243;
  final static int coulomb_units_factor                  = 242;
  final static int cull_spheres                          =  33;
  final static int cylinder_shader_ff_workaround         = 697;
  final static int cylinders_shader_filter_faces         = 687;
  final static int dash_as_cylinders                     = 684;
  final static int dash_color                            = 574;
  final static int dash_gap                              =  59;
  final static int dash_length                           =  58;
  final static int dash_radius                           = 108;
  final static int dash_round_ends                       = 280;
  final static int dash_use_shader                       = 683;
  final static int dash_width                            = 107;
  final static int debug_pick                            = 209;
  final static int default_2fofc_map_rep                 = 659;
  final static int default_buster_names                  = 657;
  final static int default_fofc_map_rep                  = 658;
  final static int default_phenix_names                  = 655;
  final static int default_phenix_no_fill_names          = 656;
  final static int default_refmac_names                  = 654;
  final static int defer_builds_mode                     = 409;
  final static int defer_updates                         = 304;
  final static int depth_cue                             =  84;
  final static int dihedral_color                        = 576;
  final static int dihedral_label_position               = 408;
  final static int dihedral_size                         = 407;
  final static int direct                                =   8;
  final static int dist_counter                          =  57;
  final static int distance_exclusion                    = 460;
  final static int dot_as_spheres                        = 701;
  final static int dot_color                             = 210;
  final static int dot_density                           =   2;
  final static int dot_hydrogens                         =  28;
  final static int dot_lighting                          = 336;
  final static int dot_mode                              =   3;
  final static int dot_normals                           = 332;
  final static int dot_radius                            =  29;
  final static int dot_solvent                           = 206;
  final static int dot_use_shader                        = 700;
  final static int dot_width                             =  77;
  final static int draw_frames                           = 436;
  final static int draw_mode                             = 614;
  final static int dump_binary                           = 749;
  final static int dynamic_measures                      = 637;
  final static int dynamic_width                         = 610;
  final static int dynamic_width_factor                  = 611;
  final static int dynamic_width_max                     = 613;
  final static int dynamic_width_min                     = 612;
  final static int edit_light                            = 707;
  final static int editor_auto_dihedral                  = 416;
  final static int editor_auto_origin                    = 439;
  final static int editor_bond_cycle_mode                = 633;
  final static int editor_label_fragments                = 321;
  final static int ellipsoid_color                       = 570;
  final static int ellipsoid_probability                 = 568;
  final static int ellipsoid_quality                     = 563;
  final static int ellipsoid_scale                       = 569;
  final static int ellipsoid_transparency                = 571;
  final static int excl_display_lists_shaders            = 682;
  final static int fast_idle                             =  54;
  final static int fetch_host                            = 636;
  final static int fetch_path                            = 507;
  final static int field_of_view                         = 152;
  final static int fit_iterations                        = 185;
  final static int fit_kabsch                            = 608;
  final static int fit_tolerance                         = 186;
  final static int float_labels                          = 232;
  final static int fog                                   =  88;
  final static int fog_start                             = 192;
  final static int frame                                 = 194;
  final static int full_screen                           = 142;
  final static int gamma                                 =  76;
  final static int gaussian_b_adjust                     = 255;
  final static int gaussian_b_floor                      = 272;
  final static int gaussian_resolution                   = 271;
  final static int geometry_export_mode                  = 586;
  final static int gl_ambient                            =  14;
  final static int gradient_max_length                   = 539;
  final static int gradient_min_length                   = 540;
  final static int gradient_min_slope                    = 541;
  final static int gradient_normal_min_dot               = 542;
  final static int gradient_spacing                      = 544;
  final static int gradient_step_size                    = 543;
  final static int gradient_symmetry                     = 545;
  final static int grid_max                              = 580;
  final static int grid_mode                             = 577;
  final static int grid_slot                             = 579;
  final static int group_arrow_prefix                    = 547;
  final static int group_auto_mode                       = 537;
  final static int group_full_member_names               = 538;
  final static int h_bond_cone                           = 286;
  final static int h_bond_cutoff_center                  = 282;
  final static int h_bond_cutoff_edge                    = 283;
  final static int h_bond_exclusion                      = 461;
  final static int h_bond_from_proton                    = 556;
  final static int h_bond_max_angle                      = 281;
  final static int h_bond_power_a                        = 284;
  final static int h_bond_power_b                        = 285;
  final static int half_bonds                            =  45;
  final static int hash_max                              =  22;
  final static int heavy_neighbor_cutoff                 = 639;
  final static int hide_long_bonds                       = 560;
  final static int hide_underscore_names                 = 458;
  final static int idle_delay                            =  52;
  final static int ignore_case                           = 414;
  final static int ignore_pdb_segi                       = 120;
  final static int image_copy_always                     = 601;
  final static int image_dots_per_inch                   = 434;
  final static int INIT                                  = 710;
  final static int internal_feedback                     = 128;
  final static int internal_gui                          =  99;
  final static int internal_gui_control_size             = 322;
  final static int internal_gui_mode                     = 341;
  final static int internal_gui_width                    =  98;
  final static int internal_prompt                       = 313;
  final static int isomesh_auto_state                    =  89;
  final static int keep_alive                            = 607;
  final static int label_anchor                          = 635;
  final static int label_angle_digits                    = 531;
  final static int label_color                           =  66;
  final static int label_digits                          = 529;
  final static int label_dihedral_digits                 = 532;
  final static int label_distance_digits                 = 530;
  final static int label_font_id                         = 328;
  final static int label_outline_color                   = 467;
  final static int label_position                        = 471;
  final static int label_shadow_mode                     = 462;
  final static int label_size                            = 453;
  final static int legacy_mouse_zoom                     = 442;
  final static int legacy_vdw_radii                      = 177;
  final static int light                                 =  10;
  final static int light_count                           = 455;
  final static int light2                                = 456;
  final static int light3                                = 457;
  final static int light4                                = 463;
  final static int light5                                = 464;
  final static int light6                                = 465;
  final static int light7                                = 466;
  final static int light8                                = 489;
  final static int light9                                = 490;
  final static int line_as_cylinders                     = 679;
  final static int line_color                            = 526;
  final static int line_radius                           = 110;
  final static int line_smooth                           =  43;
  final static int line_stick_helper                     = 391;
  final static int line_use_shader                       = 645;
  final static int line_width                            =  44;
  final static int log_box_selections                    = 133;
  final static int log_conformations                     = 134;
  final static int logging                               = 131;
  final static int map_auto_expand_sym                   = 600;
  final static int matrix_mode                           = 438;
  final static int max_threads                           = 261;
  final static int max_triangles                         =  83;
  final static int max_ups                               = 602;
  final static int mesh_as_cylinders                     = 678;
  final static int mesh_carve_cutoff                     = 591;
  final static int mesh_carve_selection                  = 589;
  final static int mesh_carve_state                      = 590;
  final static int mesh_clear_cutoff                     = 594;
  final static int mesh_clear_selection                  = 592;
  final static int mesh_clear_state                      = 593;
  final static int mesh_color                            = 146;
  final static int mesh_cutoff                           = 588;
  final static int mesh_grid_max                         = 595;
  final static int mesh_lighting                         = 337;
  final static int mesh_mode                             = 145;
  final static int mesh_negative_color                   = 536;
  final static int mesh_negative_visible                 = 535;
  final static int mesh_normals                          = 334;
  final static int mesh_quality                          = 204;
  final static int mesh_radius                           =  74;
  final static int mesh_skip                             = 528;
  final static int mesh_solvent                          = 205;
  final static int mesh_type                             = 335;
  final static int mesh_use_shader                       = 672;
  final static int mesh_width                            =  90;
  final static int min_mesh_spacing                      =   1;
  final static int moe_separate_chains                   = 558;
  final static int motion_bias                           = 628;
  final static int motion_hand                           = 631;
  final static int motion_linear                         = 630;
  final static int motion_power                          = 627;
  final static int motion_simple                         = 629;
  final static int mouse_grid                            = 587;
  final static int mouse_limit                           = 211;
  final static int mouse_restart_movie_delay             = 404;
  final static int mouse_scale                           = 212;
  final static int mouse_selection_mode                  = 354;
  final static int mouse_wheel_scale                     = 523;
  final static int mouse_z_scale                         = 619;
  final static int movie_animate_by_frame                = 565;
  final static int movie_auto_interpolate                = 621;
  final static int movie_auto_store                      = 620;
  final static int movie_delay                           =  16;
  final static int movie_fps                             = 550;
  final static int movie_loop                            = 299;
  final static int movie_panel                           = 618;
  final static int movie_panel_row_height                = 622;
  final static int movie_quality                         = 634;
  final static int movie_rock                            = 572;
  final static int multiplex                             = 385;
  final static int nb_spheres_quality                    = 689;
  final static int nb_spheres_size                       = 688;
  final static int nb_spheres_use_shader                 = 690;
  final static int neighbor_cutoff                       = 638;
  final static int no_idle                               =  53;
  final static int nonbonded_as_cylinders                = 686;
  final static int nonbonded_size                        =  65;
  final static int nonbonded_transparency                = 524;
  final static int nonbonded_use_shader                  = 685;
  final static int normal_workaround                     =  40;
  final static int normalize_ccp4_maps                   = 126;
  final static int normalize_grd_maps                    = 314;
  final static int normalize_o_maps                      = 305;
  final static int nvidia_bugs                           = 433;
  final static int offscreen_rendering_for_antialiasing  = 695;
  final static int offscreen_rendering_multiplier        = 696;
  final static int opaque_background                     = 435;
  final static int orthoscopic                           =  23;
  final static int overlay                               =  61;
  final static int overlay_lines                         = 311;
  final static int pdb_conect_all                        = 329;
  final static int pdb_discrete_chains                   = 479;
  final static int pdb_echo_tags                         = 486;
  final static int pdb_formal_charges                    = 584;
  final static int pdb_hetatm_guess_valences             = 562;
  final static int pdb_hetatm_sort                       = 267;
  final static int pdb_honor_model_number                = 424;
  final static int pdb_ignore_conect                     = 632;
  final static int pdb_insertions_go_first               = 307;
  final static int pdb_insure_orthogonal                 = 374;
  final static int pdb_literal_names                     = 190;
  final static int pdb_no_end_record                     = 301;
  final static int pdb_reformat_names_mode               = 326;
  final static int pdb_retain_ids                        = 300;
  final static int pdb_standard_order                    = 256;
  final static int pdb_truncate_residue_name             = 399;
  final static int pdb_unbond_cations                    = 480;
  final static int pdb_use_ter_records                   = 268;
  final static int pickable                              =  50;
  final static int png_file_gamma                        = 320;
  final static int png_screen_gamma                      = 319;
  final static int polar_neighbor_cutoff                 = 640;
  final static int power                                 =  11;
  final static int pqr_workarounds                       = 387;
  final static int presentation                          = 397;
  final static int presentation_auto_quit                = 415;
  final static int presentation_auto_start               = 417;
  final static int presentation_mode                     = 398;
  final static int preserve_chempy_ids                   = 154;
  final static int pymol_space_max_blue                  = 217;
  final static int pymol_space_max_green                 = 216;
  final static int pymol_space_max_red                   = 215;
  final static int pymol_space_min_factor                = 218;
  final static int raise_exceptions                      = 159;
  final static int ramp_blend_nearby_colors              = 566;
  final static int rank_assisted_sorts                   = 425;
  final static int ray_blend_blue                        = 318;
  final static int ray_blend_colors                      = 315;
  final static int ray_blend_green                       = 317;
  final static int ray_blend_red                         = 316;
  final static int ray_clip_shadows                      = 522;
  final static int ray_color_ramps                       = 509;
  final static int ray_default_renderer                  = 151;
  final static int ray_direct_shade                      = 375;
  final static int ray_hint_camera                       = 510;
  final static int ray_hint_shadow                       = 511;
  final static int ray_improve_shadows                   = 149;
  final static int ray_interior_color                    = 240;
  final static int ray_interior_mode                     = 476;
  final static int ray_interior_reflect                  = 340;
  final static int ray_interior_shadows                  = 244;
  final static int ray_interior_texture                  = 245;
  final static int ray_label_specular                    = 527;
  final static int ray_legacy_lighting                   = 477;
  final static int ray_max_passes                        = 350;
  final static int ray_opaque_background                 = 137;
  final static int ray_orthoscopic                       = 392;
  final static int ray_oversample_cutoff                 = 270;
  final static int ray_pixel_scale                       = 327;
  final static int ray_scatter                           = 555;
  final static int ray_shadow_decay_factor               = 475;
  final static int ray_shadow_decay_range                = 491;
  final static int ray_shadow_fudge                      = 207;
  final static int ray_shadows                           = 195;
  final static int ray_spec_local                        = 525;
  final static int ray_texture                           = 139;
  final static int ray_texture_settings                  = 140;
  final static int ray_trace_color                       = 546;
  final static int ray_trace_depth_factor                = 472;
  final static int ray_trace_disco_factor                = 474;
  final static int ray_trace_fog                         =  67;
  final static int ray_trace_fog_start                   =  69;
  final static int ray_trace_frames                      =  30;
  final static int ray_trace_gain                        = 469;
  final static int ray_trace_mode                        = 468;
  final static int ray_trace_persist_cutoff              = 553;
  final static int ray_trace_slope_factor                = 473;
  final static int ray_trace_trans_cutoff                = 552;
  final static int ray_transparency_contrast             = 352;
  final static int ray_transparency_oblique              = 551;
  final static int ray_transparency_oblique_power        = 554;
  final static int ray_transparency_shadows              = 199;
  final static int ray_transparency_spec_cut             = 312;
  final static int ray_transparency_specular             = 201;
  final static int ray_triangle_fudge                    = 208;
  final static int ray_volume                            = 665;
  final static int reflect                               =   9;
  final static int reflect_power                         = 153;
  final static int render_as_cylinders                   = 691;
  final static int retain_order                          = 266;
  final static int ribbon_as_cylinders                   = 680;
  final static int ribbon_color                          = 235;
  final static int ribbon_nucleic_acid_mode              = 426;
  final static int ribbon_power                          =  17;
  final static int ribbon_power_b                        =  18;
  final static int ribbon_radius                         =  20;
  final static int ribbon_sampling                       =  19;
  final static int ribbon_side_chain_helper              = 393;
  final static int ribbon_smooth                         = 237;
  final static int ribbon_throw                          = 121;
  final static int ribbon_trace_atoms                    = 196;
  final static int ribbon_transparency                   = 666;
  final static int ribbon_use_shader                     = 681;
  final static int ribbon_width                          = 106;
  final static int robust_logs                           = 132;
  final static int rock                                  = 582;
  final static int rock_delay                            =  56;
  final static int roving_byres                          = 226;
  final static int roving_cartoon                        = 228;
  final static int roving_delay                          = 224;
  final static int roving_detail                         = 233;
  final static int roving_isomesh                        = 252;
  final static int roving_isosurface                     = 253;
  final static int roving_labels                         = 223;
  final static int roving_lines                          = 220;
  final static int roving_map1_level                     = 249;
  final static int roving_map1_name                      = 246;
  final static int roving_map2_level                     = 250;
  final static int roving_map2_name                      = 247;
  final static int roving_map3_level                     = 251;
  final static int roving_map3_name                      = 248;
  final static int roving_nb_spheres                     = 234;
  final static int roving_nonbonded                      = 231;
  final static int roving_origin                         = 219;
  final static int roving_origin_z                       = 308;
  final static int roving_origin_z_cushion               = 309;
  final static int roving_polar_contacts                 = 229;
  final static int roving_polar_cutoff                   = 230;
  final static int roving_ribbon                         = 227;
  final static int roving_selection                      = 225;
  final static int roving_spheres                        = 222;
  final static int roving_sticks                         = 221;
  final static int save_pdb_ss                           = 183;
  final static int scene_animation                       = 390;
  final static int scene_animation_duration              = 411;
  final static int scene_buttons                         = 599;
  final static int scene_buttons_mode                    = 598;
  final static int scene_current_name                    = 396;
  final static int scene_frame_mode                      = 623;
  final static int scene_loop                            = 400;
  final static int scene_restart_movie_delay             = 403;
  final static int scenes_changed                        = 254;
  final static int sculpt_angl_weight                    = 168;
  final static int sculpt_auto_center                    = 478;
  final static int sculpt_avd_excl                       = 505;
  final static int sculpt_avd_gap                        = 503;
  final static int sculpt_avd_range                      = 504;
  final static int sculpt_avd_weight                     = 502;
  final static int sculpt_bond_weight                    = 167;
  final static int sculpt_field_mask                     = 174;
  final static int sculpt_hb_overlap                     = 175;
  final static int sculpt_hb_overlap_base                = 176;
  final static int sculpt_line_weight                    = 184;
  final static int sculpt_max_max                        = 500;
  final static int sculpt_max_min                        = 499;
  final static int sculpt_max_scale                      = 497;
  final static int sculpt_max_weight                     = 498;
  final static int sculpt_memory                         = 178;
  final static int sculpt_min_max                        = 496;
  final static int sculpt_min_min                        = 495;
  final static int sculpt_min_scale                      = 493;
  final static int sculpt_min_weight                     = 494;
  final static int sculpt_nb_interval                    = 273;
  final static int sculpt_plan_weight                    = 170;
  final static int sculpt_pyra_inv_weight                = 606;
  final static int sculpt_pyra_weight                    = 169;
  final static int sculpt_tors_tolerance                 = 275;
  final static int sculpt_tors_weight                    = 274;
  final static int sculpt_tri_max                        = 484;
  final static int sculpt_tri_min                        = 483;
  final static int sculpt_tri_mode                       = 485;
  final static int sculpt_tri_scale                      = 481;
  final static int sculpt_tri_weight                     = 482;
  final static int sculpt_vdw_scale                      = 163;
  final static int sculpt_vdw_scale14                    = 164;
  final static int sculpt_vdw_vis_max                    = 447;
  final static int sculpt_vdw_vis_mid                    = 446;
  final static int sculpt_vdw_vis_min                    = 445;
  final static int sculpt_vdw_vis_mode                   = 444;
  final static int sculpt_vdw_weight                     = 165;
  final static int sculpt_vdw_weight14                   = 166;
  final static int sculpting                             = 161;
  final static int sculpting_cycles                      = 171;
  final static int sdof_drag_scale                       = 597;
  final static int secondary_structure                   = 157;
  final static int security                              = 197;
  final static int sel_counter                           =   5;
  final static int selection_overlay                     =  81;
  final static int selection_round_points                = 459;
  final static int selection_visible_only                = 470;
  final static int selection_width                       =  80;
  final static int selection_width_max                   = 394;
  final static int selection_width_scale                 = 395;
  final static int seq_view                              = 353;
  final static int seq_view_alignment                    = 513;
  final static int seq_view_color                        = 362;
  final static int seq_view_discrete_by_state            = 410;
  final static int seq_view_fill_char                    = 516;
  final static int seq_view_fill_color                   = 517;
  final static int seq_view_format                       = 357;
  final static int seq_view_label_color                  = 518;
  final static int seq_view_label_mode                   = 363;
  final static int seq_view_label_spacing                = 355;
  final static int seq_view_label_start                  = 356;
  final static int seq_view_location                     = 358;
  final static int seq_view_overlay                      = 359;
  final static int seq_view_unaligned_color              = 515;
  final static int seq_view_unaligned_mode               = 514;
  final static int session_cache_optimize                = 596;
  final static int session_changed                       = 521;
  final static int session_compression                   = 549;
  final static int session_file                          = 440;
  final static int session_migration                     = 333;
  final static int session_version_check                 = 200;
  final static int shader_path                           = 648;
  final static int shininess                             =  86;
  final static int show_alpha_checker                    = 437;
  final static int show_frame_rate                       = 617;
  final static int show_progress                         = 262;
  final static int simplify_display_lists                = 265;
  final static int single_image                          =  15;
  final static int slice_dynamic_grid                    = 372;
  final static int slice_dynamic_grid_resolution         = 373;
  final static int slice_grid                            = 371;
  final static int slice_height_map                      = 370;
  final static int slice_height_scale                    = 369;
  final static int slice_track_camera                    = 368;
  final static int slow_idle                             =  55;
  final static int smooth_color_triangle                 = 150;
  final static int smooth_half_bonds                     = 705;
  final static int solvent_radius                        =   4;
  final static int spec_count                            = 492;
  final static int spec_direct                           = 454;
  final static int spec_direct_power                     = 488;
  final static int spec_power                            =  25;
  final static int spec_reflect                          =  24;
  final static int specular                              =  85;
  final static int specular_intensity                    = 310;
  final static int sphere_color                          = 173;
  final static int sphere_mode                           = 421;
  final static int sphere_point_max_size                 = 422;
  final static int sphere_point_size                     = 423;
  final static int sphere_quality                        =  87;
  final static int sphere_scale                          = 155;
  final static int sphere_solvent                        = 203;
  final static int sphere_transparency                   = 172;
  final static int sphere_use_shader                     = 646;
  final static int spheroid_fill                         =  71;
  final static int spheroid_scale                        =  68;
  final static int spheroid_smooth                       =  70;
  final static int ss_helix_phi_exclude                  = 292;
  final static int ss_helix_phi_include                  = 291;
  final static int ss_helix_phi_target                   = 290;
  final static int ss_helix_psi_exclude                  = 289;
  final static int ss_helix_psi_include                  = 288;
  final static int ss_helix_psi_target                   = 287;
  final static int ss_strand_phi_exclude                 = 298;
  final static int ss_strand_phi_include                 = 297;
  final static int ss_strand_phi_target                  = 296;
  final static int ss_strand_psi_exclude                 = 295;
  final static int ss_strand_psi_include                 = 294;
  final static int ss_strand_psi_target                  = 293;
  final static int state                                 = 193;
  final static int state_counter_mode                    = 667;
  final static int static_singletons                     =  82;
  final static int stereo                                = 365;
  final static int stereo_angle                          =  41;
  final static int stereo_double_pump_mono               = 202;
  final static int stereo_dynamic_strength               = 609;
  final static int stereo_mode                           = 188;
  final static int stereo_shift                          =  42;
  final static int stick_as_cylinders                    = 677;
  final static int stick_ball                            = 276;
  final static int stick_ball_color                      = 604;
  final static int stick_ball_ratio                      = 277;
  final static int stick_color                           = 376;
  final static int stick_debug                           = 673;
  final static int stick_fixed_radius                    = 278;
  final static int stick_good_geometry                   = 676;
  final static int stick_h_scale                         = 605;
  final static int stick_nub                             =  48;
  final static int stick_overlap                         =  47;
  final static int stick_quality                         =  46;
  final static int stick_radius                          =  21;
  final static int stick_round_nub                       = 675;
  final static int stick_transparency                    = 198;
  final static int stick_use_shader                      = 644;
  final static int stick_valence_scale                   = 512;
  final static int stop_on_exceptions                    = 160;
  final static int suppress_hidden                       = 548;
  final static int surface_best                          =  36;
  final static int surface_carve_cutoff                  = 344;
  final static int surface_carve_normal_cutoff           = 519;
  final static int surface_carve_selection               = 342;
  final static int surface_carve_state                   = 343;
  final static int surface_cavity_cutoff                 = 626;
  final static int surface_cavity_mode                   = 624;
  final static int surface_cavity_radius                 = 625;
  final static int surface_circumscribe                  = 501;
  final static int surface_clear_cutoff                  = 347;
  final static int surface_clear_selection               = 345;
  final static int surface_clear_state                   = 346;
  final static int surface_color                         = 144;
  final static int surface_color_smoothing               = 698;
  final static int surface_color_smoothing_threshold     = 699;
  final static int surface_debug                         = 148;
  final static int surface_miserable                     = 136;
  final static int surface_mode                          = 143;
  final static int surface_negative_color                = 534;
  final static int surface_negative_visible              = 533;
  final static int surface_normal                        =  37;
  final static int surface_optimize_subsets              = 384;
  final static int surface_poor                          = 127;
  final static int surface_proximity                     =  39;
  final static int surface_quality                       =  38;
  final static int surface_ramp_above_mode               = 364;
  final static int surface_residue_cutoff                = 641;
  final static int surface_solvent                       = 338;
  final static int surface_trim_cutoff                   = 348;
  final static int surface_trim_factor                   = 349;
  final static int surface_type                          = 331;
  final static int surface_use_shader                    = 642;
  final static int suspend_undo                          = 708;
  final static int suspend_undo_atom_count               = 709;
  final static int suspend_updates                       = 141;
  final static int swap_dsn6_bytes                       = 306;
  final static int sweep_angle                           =  26;
  final static int sweep_mode                            = 401;
  final static int sweep_phase                           = 402;
  final static int sweep_speed                           =  27;
  final static int test1                                 =  34;
  final static int test2                                 =  35;
  final static int text                                  =  62;
  final static int texture_fonts                         = 386;
  final static int trace_atoms_mode                      = 520;
  final static int transparency                          = 138;
  final static int transparency_global_sort              = 559;
  final static int transparency_mode                     = 213;
  final static int transparency_picking_mode             = 324;
  final static int triangle_max_passes                   = 339;
  final static int trim_dots                             =  32;
  final static int two_sided_lighting                    = 156;
  final static int unused_boolean_def_true               = 419;
  final static int use_display_lists                     = 263;
  final static int use_shaders                           = 647;
  final static int valence                               =  64;
  final static int valence_mode                          = 616;
  final static int valence_size                          = 135;
  final static int validate_object_names                 = 418;
  final static int virtual_trackball                     = 325;
  final static int volume_bit_depth                      = 649;
  final static int volume_color                          = 650;
  final static int volume_data_range                     = 652;
  final static int volume_layers                         = 651;
  final static int wildcard                              = 412;
  final static int wizard_prompt_mode                    = 366;
  final static int wrap_output                           = 191;
  
  final static int COLOR_FRONT = -6;
  final static int COLOR_BACK = -7;
  final static int COLOR_BLACK = 1;
  
  private final static int[] colors = {
    /* 0     */ 0xFFFFFFFF, 
    /* 1     */ 0xFF000000, 
    /* 2     */ 0xFF0000FF, 
    /* 3     */ 0xFF00FF00, 
    /* 4     */ 0xFFFF0000, 
    /* 5     */ 0xFF00FFFF, 
    /* 6     */ 0xFFFFFF00, 
    /* 7     */ 0xFFFFFF00, 
    /* 8     */ 0xFFFF00FF, 
    /* 9     */ 0xFFFF9999, 
    /* 10    */ 0xFF7FFF7F, 
    /* 11    */ 0xFF7F7FFF, 
    /* 12    */ 0xFFFF007F, 
    /* 13    */ 0xFFFF7F00, 
    /* 14    */ 0xFF7FFF00, 
    /* 15    */ 0xFF00FF7F, 
    /* 16    */ 0xFF7F00FF, 
    /* 17    */ 0xFF007FFF, 
    /* 18    */ 0xFFC4B200, 
    /* 19    */ 0xFFBF00BF, 
    /* 20    */ 0xFF00BFBF, 
    /* 21    */ 0xFF993333, 
    /* 22    */ 0xFF339933, 
    /* 23    */ 0xFF3F3FA5, 
    /* 24    */ 0xFF7F7F7F, 
    /* 25    */ 0xFF7F7F7F, 
    /* 26    */ 0xFF33FF33, 
    /* 27    */ 0xFF3333FF, 
    /* 28    */ 0xFFFF4C4C, 
    /* 29    */ 0xFFE5E5E5, 
    /* 30    */ 0xFFFFB233, 
    /* 31    */ 0xFFE5C53F, 
    /* 32    */ 0xFFFF3333, 
    /* 33    */ 0xFF33FF33, 
    /* 34    */ 0xFF4C4CFF, 
    /* 35    */ 0xFFFFFF33, 
    /* 36    */ 0xFFFFDD5E, 
    /* 37    */ 0xFFFF8C26, 
    /* 38    */ 0xFF1919FF, 
    /* 39    */ 0xFF3319E5, 
    /* 40    */ 0xFF4C19CC, 
    /* 41    */ 0xFF6619B2, 
    /* 42    */ 0xFF7F1999, 
    /* 43    */ 0xFF99197F, 
    /* 44    */ 0xFFB21966, 
    /* 45    */ 0xFFCC194C, 
    /* 46    */ 0xFFE51933, 
    /* 47    */ 0xFFFF1919, 
    /* 48    */ 0xFFFFA5D8, 
    /* 49    */ 0xFFB12121, 
    /* 50    */ 0xFF8D381C, 
    /* 51    */ 0xFFA5512B, 
    /* 52    */ 0xFFFCD1A5, 
    /* 53    */ 0xFFFF7FFF, 
    /* 54    */ 0xFF000000, 
    /* 55    */ 0xFF020202, 
    /* 56    */ 0xFF050505, 
    /* 57    */ 0xFF070707, 
    /* 58    */ 0xFF0A0A0A, 
    /* 59    */ 0xFF0C0C0C, 
    /* 60    */ 0xFF0F0F0F, 
    /* 61    */ 0xFF121212, 
    /* 62    */ 0xFF141414, 
    /* 63    */ 0xFF171717, 
    /* 64    */ 0xFF191919, 
    /* 65    */ 0xFF1C1C1C, 
    /* 66    */ 0xFF1E1E1E, 
    /* 67    */ 0xFF212121, 
    /* 68    */ 0xFF242424, 
    /* 69    */ 0xFF262626, 
    /* 70    */ 0xFF292929, 
    /* 71    */ 0xFF2B2B2B, 
    /* 72    */ 0xFF2E2E2E, 
    /* 73    */ 0xFF303030, 
    /* 74    */ 0xFF333333, 
    /* 75    */ 0xFF363636, 
    /* 76    */ 0xFF383838, 
    /* 77    */ 0xFF3B3B3B, 
    /* 78    */ 0xFF3D3D3D, 
    /* 79    */ 0xFF404040, 
    /* 80    */ 0xFF424242, 
    /* 81    */ 0xFF454545, 
    /* 82    */ 0xFF484848, 
    /* 83    */ 0xFF4A4A4A, 
    /* 84    */ 0xFF4D4D4D, 
    /* 85    */ 0xFF4F4F4F, 
    /* 86    */ 0xFF525252, 
    /* 87    */ 0xFF555555, 
    /* 88    */ 0xFF575757, 
    /* 89    */ 0xFF5A5A5A, 
    /* 90    */ 0xFF5C5C5C, 
    /* 91    */ 0xFF5F5F5F, 
    /* 92    */ 0xFF616161, 
    /* 93    */ 0xFF646464, 
    /* 94    */ 0xFF676767, 
    /* 95    */ 0xFF696969, 
    /* 96    */ 0xFF6C6C6C, 
    /* 97    */ 0xFF6E6E6E, 
    /* 98    */ 0xFF717171, 
    /* 99    */ 0xFF737373, 
    /* 100   */ 0xFF767676, 
    /* 101   */ 0xFF797979, 
    /* 102   */ 0xFF7B7B7B, 
    /* 103   */ 0xFF7E7E7E, 
    /* 104   */ 0xFF808080, 
    /* 105   */ 0xFF838383, 
    /* 106   */ 0xFF858585, 
    /* 107   */ 0xFF888888, 
    /* 108   */ 0xFF8B8B8B, 
    /* 109   */ 0xFF8D8D8D, 
    /* 110   */ 0xFF909090, 
    /* 111   */ 0xFF929292, 
    /* 112   */ 0xFF959595, 
    /* 113   */ 0xFF979797, 
    /* 114   */ 0xFF9A9A9A, 
    /* 115   */ 0xFF9D9D9D, 
    /* 116   */ 0xFF9F9F9F, 
    /* 117   */ 0xFFA2A2A2, 
    /* 118   */ 0xFFA4A4A4, 
    /* 119   */ 0xFFA7A7A7, 
    /* 120   */ 0xFFAAAAAA, 
    /* 121   */ 0xFFACACAC, 
    /* 122   */ 0xFFAFAFAF, 
    /* 123   */ 0xFFB1B1B1, 
    /* 124   */ 0xFFB4B4B4, 
    /* 125   */ 0xFFB6B6B6, 
    /* 126   */ 0xFFB9B9B9, 
    /* 127   */ 0xFFBCBCBC, 
    /* 128   */ 0xFFBEBEBE, 
    /* 129   */ 0xFFC1C1C1, 
    /* 130   */ 0xFFC3C3C3, 
    /* 131   */ 0xFFC6C6C6, 
    /* 132   */ 0xFFC8C8C8, 
    /* 133   */ 0xFFCBCBCB, 
    /* 134   */ 0xFFCECECE, 
    /* 135   */ 0xFFD0D0D0, 
    /* 136   */ 0xFFD3D3D3, 
    /* 137   */ 0xFFD5D5D5, 
    /* 138   */ 0xFFD8D8D8, 
    /* 139   */ 0xFFDADADA, 
    /* 140   */ 0xFFDDDDDD, 
    /* 141   */ 0xFFE0E0E0, 
    /* 142   */ 0xFFE2E2E2, 
    /* 143   */ 0xFFE5E5E5, 
    /* 144   */ 0xFFE7E7E7, 
    /* 145   */ 0xFFEAEAEA, 
    /* 146   */ 0xFFECECEC, 
    /* 147   */ 0xFFEFEFEF, 
    /* 148   */ 0xFFF2F2F2, 
    /* 149   */ 0xFFF4F4F4, 
    /* 150   */ 0xFFF7F7F7, 
    /* 151   */ 0xFFF9F9F9, 
    /* 152   */ 0xFFFCFCFC, 
    /* 153   */ 0xFFFFFFFF, 
    /* 154   */ 0xFFFF33CC, 
    /* 155   */ 0xFFFF00FF, 
    /* 156   */ 0xFFFD00FF, 
    /* 157   */ 0xFFFB00FF, 
    /* 158   */ 0xFFFA00FF, 
    /* 159   */ 0xFFF800FF, 
    /* 160   */ 0xFFF700FF, 
    /* 161   */ 0xFFF500FF, 
    /* 162   */ 0xFFF400FF, 
    /* 163   */ 0xFFF200FF, 
    /* 164   */ 0xFFF100FF, 
    /* 165   */ 0xFFEF00FF, 
    /* 166   */ 0xFFEE00FF, 
    /* 167   */ 0xFFEC00FF, 
    /* 168   */ 0xFFEB00FF, 
    /* 169   */ 0xFFE900FF, 
    /* 170   */ 0xFFE800FF, 
    /* 171   */ 0xFFE600FF, 
    /* 172   */ 0xFFE400FF, 
    /* 173   */ 0xFFE300FF, 
    /* 174   */ 0xFFE100FF, 
    /* 175   */ 0xFFE000FF, 
    /* 176   */ 0xFFDE00FF, 
    /* 177   */ 0xFFDD00FF, 
    /* 178   */ 0xFFDB00FF, 
    /* 179   */ 0xFFDA00FF, 
    /* 180   */ 0xFFD800FF, 
    /* 181   */ 0xFFD700FF, 
    /* 182   */ 0xFFD500FF, 
    /* 183   */ 0xFFD400FF, 
    /* 184   */ 0xFFD200FF, 
    /* 185   */ 0xFFD100FF, 
    /* 186   */ 0xFFCF00FF, 
    /* 187   */ 0xFFCE00FF, 
    /* 188   */ 0xFFCC00FF, 
    /* 189   */ 0xFFCA00FF, 
    /* 190   */ 0xFFC900FF, 
    /* 191   */ 0xFFC700FF, 
    /* 192   */ 0xFFC600FF, 
    /* 193   */ 0xFFC400FF, 
    /* 194   */ 0xFFC300FF, 
    /* 195   */ 0xFFC100FF, 
    /* 196   */ 0xFFC000FF, 
    /* 197   */ 0xFFBE00FF, 
    /* 198   */ 0xFFBD00FF, 
    /* 199   */ 0xFFBB00FF, 
    /* 200   */ 0xFFBA00FF, 
    /* 201   */ 0xFFB800FF, 
    /* 202   */ 0xFFB700FF, 
    /* 203   */ 0xFFB500FF, 
    /* 204   */ 0xFFB400FF, 
    /* 205   */ 0xFFB200FF, 
    /* 206   */ 0xFFB000FF, 
    /* 207   */ 0xFFAF00FF, 
    /* 208   */ 0xFFAD00FF, 
    /* 209   */ 0xFFAC00FF, 
    /* 210   */ 0xFFAA00FF, 
    /* 211   */ 0xFFA900FF, 
    /* 212   */ 0xFFA700FF, 
    /* 213   */ 0xFFA600FF, 
    /* 214   */ 0xFFA400FF, 
    /* 215   */ 0xFFA300FF, 
    /* 216   */ 0xFFA100FF, 
    /* 217   */ 0xFFA000FF, 
    /* 218   */ 0xFF9E00FF, 
    /* 219   */ 0xFF9D00FF, 
    /* 220   */ 0xFF9B00FF, 
    /* 221   */ 0xFF9A00FF, 
    /* 222   */ 0xFF9800FF, 
    /* 223   */ 0xFF9600FF, 
    /* 224   */ 0xFF9500FF, 
    /* 225   */ 0xFF9300FF, 
    /* 226   */ 0xFF9200FF, 
    /* 227   */ 0xFF9000FF, 
    /* 228   */ 0xFF8F00FF, 
    /* 229   */ 0xFF8D00FF, 
    /* 230   */ 0xFF8C00FF, 
    /* 231   */ 0xFF8A00FF, 
    /* 232   */ 0xFF8900FF, 
    /* 233   */ 0xFF8700FF, 
    /* 234   */ 0xFF8600FF, 
    /* 235   */ 0xFF8400FF, 
    /* 236   */ 0xFF8300FF, 
    /* 237   */ 0xFF8100FF, 
    /* 238   */ 0xFF8000FF, 
    /* 239   */ 0xFF7E00FF, 
    /* 240   */ 0xFF7C00FF, 
    /* 241   */ 0xFF7B00FF, 
    /* 242   */ 0xFF7900FF, 
    /* 243   */ 0xFF7800FF, 
    /* 244   */ 0xFF7600FF, 
    /* 245   */ 0xFF7500FF, 
    /* 246   */ 0xFF7300FF, 
    /* 247   */ 0xFF7200FF, 
    /* 248   */ 0xFF7000FF, 
    /* 249   */ 0xFF6F00FF, 
    /* 250   */ 0xFF6D00FF, 
    /* 251   */ 0xFF6C00FF, 
    /* 252   */ 0xFF6A00FF, 
    /* 253   */ 0xFF6900FF, 
    /* 254   */ 0xFF6700FF, 
    /* 255   */ 0xFF6600FF, 
    /* 256   */ 0xFF6400FF, 
    /* 257   */ 0xFF6200FF, 
    /* 258   */ 0xFF6100FF, 
    /* 259   */ 0xFF5F00FF, 
    /* 260   */ 0xFF5E00FF, 
    /* 261   */ 0xFF5C00FF, 
    /* 262   */ 0xFF5B00FF, 
    /* 263   */ 0xFF5900FF, 
    /* 264   */ 0xFF5800FF, 
    /* 265   */ 0xFF5600FF, 
    /* 266   */ 0xFF5500FF, 
    /* 267   */ 0xFF5300FF, 
    /* 268   */ 0xFF5200FF, 
    /* 269   */ 0xFF5000FF, 
    /* 270   */ 0xFF4F00FF, 
    /* 271   */ 0xFF4D00FF, 
    /* 272   */ 0xFF4B00FF, 
    /* 273   */ 0xFF4A00FF, 
    /* 274   */ 0xFF4800FF, 
    /* 275   */ 0xFF4700FF, 
    /* 276   */ 0xFF4500FF, 
    /* 277   */ 0xFF4400FF, 
    /* 278   */ 0xFF4200FF, 
    /* 279   */ 0xFF4100FF, 
    /* 280   */ 0xFF3F00FF, 
    /* 281   */ 0xFF3E00FF, 
    /* 282   */ 0xFF3C00FF, 
    /* 283   */ 0xFF3B00FF, 
    /* 284   */ 0xFF3900FF, 
    /* 285   */ 0xFF3800FF, 
    /* 286   */ 0xFF3600FF, 
    /* 287   */ 0xFF3500FF, 
    /* 288   */ 0xFF3300FF, 
    /* 289   */ 0xFF3100FF, 
    /* 290   */ 0xFF3000FF, 
    /* 291   */ 0xFF2E00FF, 
    /* 292   */ 0xFF2D00FF, 
    /* 293   */ 0xFF2B00FF, 
    /* 294   */ 0xFF2A00FF, 
    /* 295   */ 0xFF2800FF, 
    /* 296   */ 0xFF2700FF, 
    /* 297   */ 0xFF2500FF, 
    /* 298   */ 0xFF2400FF, 
    /* 299   */ 0xFF2200FF, 
    /* 300   */ 0xFF2100FF, 
    /* 301   */ 0xFF1F00FF, 
    /* 302   */ 0xFF1E00FF, 
    /* 303   */ 0xFF1C00FF, 
    /* 304   */ 0xFF1B00FF, 
    /* 305   */ 0xFF1900FF, 
    /* 306   */ 0xFF1700FF, 
    /* 307   */ 0xFF1600FF, 
    /* 308   */ 0xFF1400FF, 
    /* 309   */ 0xFF1300FF, 
    /* 310   */ 0xFF1100FF, 
    /* 311   */ 0xFF1000FF, 
    /* 312   */ 0xFF0E00FF, 
    /* 313   */ 0xFF0D00FF, 
    /* 314   */ 0xFF0B00FF, 
    /* 315   */ 0xFF0A00FF, 
    /* 316   */ 0xFF0800FF, 
    /* 317   */ 0xFF0700FF, 
    /* 318   */ 0xFF0500FF, 
    /* 319   */ 0xFF0400FF, 
    /* 320   */ 0xFF0200FF, 
    /* 321   */ 0xFF0100FF, 
    /* 322   */ 0xFF0000FF, 
    /* 323   */ 0xFF0002FF, 
    /* 324   */ 0xFF0003FF, 
    /* 325   */ 0xFF0005FF, 
    /* 326   */ 0xFF0006FF, 
    /* 327   */ 0xFF0008FF, 
    /* 328   */ 0xFF0009FF, 
    /* 329   */ 0xFF000BFF, 
    /* 330   */ 0xFF000CFF, 
    /* 331   */ 0xFF000EFF, 
    /* 332   */ 0xFF000FFF, 
    /* 333   */ 0xFF0011FF, 
    /* 334   */ 0xFF0012FF, 
    /* 335   */ 0xFF0014FF, 
    /* 336   */ 0xFF0015FF, 
    /* 337   */ 0xFF0017FF, 
    /* 338   */ 0xFF0018FF, 
    /* 339   */ 0xFF001AFF, 
    /* 340   */ 0xFF001CFF, 
    /* 341   */ 0xFF001DFF, 
    /* 342   */ 0xFF001FFF, 
    /* 343   */ 0xFF0020FF, 
    /* 344   */ 0xFF0022FF, 
    /* 345   */ 0xFF0023FF, 
    /* 346   */ 0xFF0025FF, 
    /* 347   */ 0xFF0026FF, 
    /* 348   */ 0xFF0028FF, 
    /* 349   */ 0xFF0029FF, 
    /* 350   */ 0xFF002BFF, 
    /* 351   */ 0xFF002CFF, 
    /* 352   */ 0xFF002EFF, 
    /* 353   */ 0xFF002FFF, 
    /* 354   */ 0xFF0031FF, 
    /* 355   */ 0xFF0032FF, 
    /* 356   */ 0xFF0034FF, 
    /* 357   */ 0xFF0036FF, 
    /* 358   */ 0xFF0037FF, 
    /* 359   */ 0xFF0039FF, 
    /* 360   */ 0xFF003AFF, 
    /* 361   */ 0xFF003CFF, 
    /* 362   */ 0xFF003DFF, 
    /* 363   */ 0xFF003FFF, 
    /* 364   */ 0xFF0040FF, 
    /* 365   */ 0xFF0042FF, 
    /* 366   */ 0xFF0043FF, 
    /* 367   */ 0xFF0045FF, 
    /* 368   */ 0xFF0046FF, 
    /* 369   */ 0xFF0048FF, 
    /* 370   */ 0xFF0049FF, 
    /* 371   */ 0xFF004BFF, 
    /* 372   */ 0xFF004DFF, 
    /* 373   */ 0xFF004EFF, 
    /* 374   */ 0xFF0050FF, 
    /* 375   */ 0xFF0051FF, 
    /* 376   */ 0xFF0053FF, 
    /* 377   */ 0xFF0054FF, 
    /* 378   */ 0xFF0056FF, 
    /* 379   */ 0xFF0057FF, 
    /* 380   */ 0xFF0059FF, 
    /* 381   */ 0xFF005AFF, 
    /* 382   */ 0xFF005CFF, 
    /* 383   */ 0xFF005DFF, 
    /* 384   */ 0xFF005FFF, 
    /* 385   */ 0xFF0060FF, 
    /* 386   */ 0xFF0062FF, 
    /* 387   */ 0xFF0063FF, 
    /* 388   */ 0xFF0065FF, 
    /* 389   */ 0xFF0067FF, 
    /* 390   */ 0xFF0068FF, 
    /* 391   */ 0xFF006AFF, 
    /* 392   */ 0xFF006BFF, 
    /* 393   */ 0xFF006DFF, 
    /* 394   */ 0xFF006EFF, 
    /* 395   */ 0xFF0070FF, 
    /* 396   */ 0xFF0071FF, 
    /* 397   */ 0xFF0073FF, 
    /* 398   */ 0xFF0074FF, 
    /* 399   */ 0xFF0076FF, 
    /* 400   */ 0xFF0077FF, 
    /* 401   */ 0xFF0079FF, 
    /* 402   */ 0xFF007AFF, 
    /* 403   */ 0xFF007CFF, 
    /* 404   */ 0xFF007DFF, 
    /* 405   */ 0xFF007FFF, 
    /* 406   */ 0xFF0081FF, 
    /* 407   */ 0xFF0082FF, 
    /* 408   */ 0xFF0084FF, 
    /* 409   */ 0xFF0085FF, 
    /* 410   */ 0xFF0087FF, 
    /* 411   */ 0xFF0088FF, 
    /* 412   */ 0xFF008AFF, 
    /* 413   */ 0xFF008BFF, 
    /* 414   */ 0xFF008DFF, 
    /* 415   */ 0xFF008EFF, 
    /* 416   */ 0xFF0090FF, 
    /* 417   */ 0xFF0091FF, 
    /* 418   */ 0xFF0093FF, 
    /* 419   */ 0xFF0094FF, 
    /* 420   */ 0xFF0096FF, 
    /* 421   */ 0xFF0097FF, 
    /* 422   */ 0xFF0099FF, 
    /* 423   */ 0xFF009BFF, 
    /* 424   */ 0xFF009CFF, 
    /* 425   */ 0xFF009EFF, 
    /* 426   */ 0xFF009FFF, 
    /* 427   */ 0xFF00A1FF, 
    /* 428   */ 0xFF00A2FF, 
    /* 429   */ 0xFF00A4FF, 
    /* 430   */ 0xFF00A5FF, 
    /* 431   */ 0xFF00A7FF, 
    /* 432   */ 0xFF00A8FF, 
    /* 433   */ 0xFF00AAFF, 
    /* 434   */ 0xFF00ABFF, 
    /* 435   */ 0xFF00ADFF, 
    /* 436   */ 0xFF00AEFF, 
    /* 437   */ 0xFF00B0FF, 
    /* 438   */ 0xFF00B1FF, 
    /* 439   */ 0xFF00B3FF, 
    /* 440   */ 0xFF00B5FF, 
    /* 441   */ 0xFF00B6FF, 
    /* 442   */ 0xFF00B8FF, 
    /* 443   */ 0xFF00B9FF, 
    /* 444   */ 0xFF00BBFF, 
    /* 445   */ 0xFF00BCFF, 
    /* 446   */ 0xFF00BEFF, 
    /* 447   */ 0xFF00BFFF, 
    /* 448   */ 0xFF00C1FF, 
    /* 449   */ 0xFF00C2FF, 
    /* 450   */ 0xFF00C4FF, 
    /* 451   */ 0xFF00C5FF, 
    /* 452   */ 0xFF00C7FF, 
    /* 453   */ 0xFF00C8FF, 
    /* 454   */ 0xFF00CAFF, 
    /* 455   */ 0xFF00CBFF, 
    /* 456   */ 0xFF00CDFF, 
    /* 457   */ 0xFF00CFFF, 
    /* 458   */ 0xFF00D0FF, 
    /* 459   */ 0xFF00D2FF, 
    /* 460   */ 0xFF00D3FF, 
    /* 461   */ 0xFF00D5FF, 
    /* 462   */ 0xFF00D6FF, 
    /* 463   */ 0xFF00D8FF, 
    /* 464   */ 0xFF00D9FF, 
    /* 465   */ 0xFF00DBFF, 
    /* 466   */ 0xFF00DCFF, 
    /* 467   */ 0xFF00DEFF, 
    /* 468   */ 0xFF00DFFF, 
    /* 469   */ 0xFF00E1FF, 
    /* 470   */ 0xFF00E2FF, 
    /* 471   */ 0xFF00E4FF, 
    /* 472   */ 0xFF00E6FF, 
    /* 473   */ 0xFF00E7FF, 
    /* 474   */ 0xFF00E9FF, 
    /* 475   */ 0xFF00EAFF, 
    /* 476   */ 0xFF00ECFF, 
    /* 477   */ 0xFF00EDFF, 
    /* 478   */ 0xFF00EFFF, 
    /* 479   */ 0xFF00F0FF, 
    /* 480   */ 0xFF00F2FF, 
    /* 481   */ 0xFF00F3FF, 
    /* 482   */ 0xFF00F5FF, 
    /* 483   */ 0xFF00F6FF, 
    /* 484   */ 0xFF00F8FF, 
    /* 485   */ 0xFF00F9FF, 
    /* 486   */ 0xFF00FBFF, 
    /* 487   */ 0xFF00FCFF, 
    /* 488   */ 0xFF00FEFF, 
    /* 489   */ 0xFF00FFFD, 
    /* 490   */ 0xFF00FFFC, 
    /* 491   */ 0xFF00FFFA, 
    /* 492   */ 0xFF00FFF9, 
    /* 493   */ 0xFF00FFF7, 
    /* 494   */ 0xFF00FFF6, 
    /* 495   */ 0xFF00FFF4, 
    /* 496   */ 0xFF00FFF3, 
    /* 497   */ 0xFF00FFF1, 
    /* 498   */ 0xFF00FFF0, 
    /* 499   */ 0xFF00FFEE, 
    /* 500   */ 0xFF00FFED, 
    /* 501   */ 0xFF00FFEB, 
    /* 502   */ 0xFF00FFEA, 
    /* 503   */ 0xFF00FFE8, 
    /* 504   */ 0xFF00FFE7, 
    /* 505   */ 0xFF00FFE5, 
    /* 506   */ 0xFF00FFE3, 
    /* 507   */ 0xFF00FFE2, 
    /* 508   */ 0xFF00FFE0, 
    /* 509   */ 0xFF00FFDF, 
    /* 510   */ 0xFF00FFDD, 
    /* 511   */ 0xFF00FFDC, 
    /* 512   */ 0xFF00FFDA, 
    /* 513   */ 0xFF00FFD9, 
    /* 514   */ 0xFF00FFD7, 
    /* 515   */ 0xFF00FFD6, 
    /* 516   */ 0xFF00FFD4, 
    /* 517   */ 0xFF00FFD3, 
    /* 518   */ 0xFF00FFD1, 
    /* 519   */ 0xFF00FFD0, 
    /* 520   */ 0xFF00FFCE, 
    /* 521   */ 0xFF00FFCD, 
    /* 522   */ 0xFF00FFCB, 
    /* 523   */ 0xFF00FFC9, 
    /* 524   */ 0xFF00FFC8, 
    /* 525   */ 0xFF00FFC6, 
    /* 526   */ 0xFF00FFC5, 
    /* 527   */ 0xFF00FFC3, 
    /* 528   */ 0xFF00FFC2, 
    /* 529   */ 0xFF00FFC0, 
    /* 530   */ 0xFF00FFBF, 
    /* 531   */ 0xFF00FFBD, 
    /* 532   */ 0xFF00FFBC, 
    /* 533   */ 0xFF00FFBA, 
    /* 534   */ 0xFF00FFB9, 
    /* 535   */ 0xFF00FFB7, 
    /* 536   */ 0xFF00FFB6, 
    /* 537   */ 0xFF00FFB4, 
    /* 538   */ 0xFF00FFB3, 
    /* 539   */ 0xFF00FFB1, 
    /* 540   */ 0xFF00FFAF, 
    /* 541   */ 0xFF00FFAE, 
    /* 542   */ 0xFF00FFAC, 
    /* 543   */ 0xFF00FFAB, 
    /* 544   */ 0xFF00FFA9, 
    /* 545   */ 0xFF00FFA8, 
    /* 546   */ 0xFF00FFA6, 
    /* 547   */ 0xFF00FFA5, 
    /* 548   */ 0xFF00FFA3, 
    /* 549   */ 0xFF00FFA2, 
    /* 550   */ 0xFF00FFA0, 
    /* 551   */ 0xFF00FF9F, 
    /* 552   */ 0xFF00FF9D, 
    /* 553   */ 0xFF00FF9C, 
    /* 554   */ 0xFF00FF9A, 
    /* 555   */ 0xFF00FF99, 
    /* 556   */ 0xFF00FF97, 
    /* 557   */ 0xFF00FF95, 
    /* 558   */ 0xFF00FF94, 
    /* 559   */ 0xFF00FF92, 
    /* 560   */ 0xFF00FF91, 
    /* 561   */ 0xFF00FF8F, 
    /* 562   */ 0xFF00FF8E, 
    /* 563   */ 0xFF00FF8C, 
    /* 564   */ 0xFF00FF8B, 
    /* 565   */ 0xFF00FF89, 
    /* 566   */ 0xFF00FF88, 
    /* 567   */ 0xFF00FF86, 
    /* 568   */ 0xFF00FF85, 
    /* 569   */ 0xFF00FF83, 
    /* 570   */ 0xFF00FF82, 
    /* 571   */ 0xFF00FF80, 
    /* 572   */ 0xFF00FF7E, 
    /* 573   */ 0xFF00FF7D, 
    /* 574   */ 0xFF00FF7B, 
    /* 575   */ 0xFF00FF7A, 
    /* 576   */ 0xFF00FF78, 
    /* 577   */ 0xFF00FF77, 
    /* 578   */ 0xFF00FF75, 
    /* 579   */ 0xFF00FF74, 
    /* 580   */ 0xFF00FF72, 
    /* 581   */ 0xFF00FF71, 
    /* 582   */ 0xFF00FF6F, 
    /* 583   */ 0xFF00FF6E, 
    /* 584   */ 0xFF00FF6C, 
    /* 585   */ 0xFF00FF6B, 
    /* 586   */ 0xFF00FF69, 
    /* 587   */ 0xFF00FF68, 
    /* 588   */ 0xFF00FF66, 
    /* 589   */ 0xFF00FF64, 
    /* 590   */ 0xFF00FF63, 
    /* 591   */ 0xFF00FF61, 
    /* 592   */ 0xFF00FF60, 
    /* 593   */ 0xFF00FF5E, 
    /* 594   */ 0xFF00FF5D, 
    /* 595   */ 0xFF00FF5B, 
    /* 596   */ 0xFF00FF5A, 
    /* 597   */ 0xFF00FF58, 
    /* 598   */ 0xFF00FF57, 
    /* 599   */ 0xFF00FF55, 
    /* 600   */ 0xFF00FF54, 
    /* 601   */ 0xFF00FF52, 
    /* 602   */ 0xFF00FF51, 
    /* 603   */ 0xFF00FF4F, 
    /* 604   */ 0xFF00FF4E, 
    /* 605   */ 0xFF00FF4C, 
    /* 606   */ 0xFF00FF4A, 
    /* 607   */ 0xFF00FF49, 
    /* 608   */ 0xFF00FF47, 
    /* 609   */ 0xFF00FF46, 
    /* 610   */ 0xFF00FF44, 
    /* 611   */ 0xFF00FF43, 
    /* 612   */ 0xFF00FF41, 
    /* 613   */ 0xFF00FF40, 
    /* 614   */ 0xFF00FF3E, 
    /* 615   */ 0xFF00FF3D, 
    /* 616   */ 0xFF00FF3B, 
    /* 617   */ 0xFF00FF3A, 
    /* 618   */ 0xFF00FF38, 
    /* 619   */ 0xFF00FF37, 
    /* 620   */ 0xFF00FF35, 
    /* 621   */ 0xFF00FF34, 
    /* 622   */ 0xFF00FF32, 
    /* 623   */ 0xFF00FF30, 
    /* 624   */ 0xFF00FF2F, 
    /* 625   */ 0xFF00FF2D, 
    /* 626   */ 0xFF00FF2C, 
    /* 627   */ 0xFF00FF2A, 
    /* 628   */ 0xFF00FF29, 
    /* 629   */ 0xFF00FF27, 
    /* 630   */ 0xFF00FF26, 
    /* 631   */ 0xFF00FF24, 
    /* 632   */ 0xFF00FF23, 
    /* 633   */ 0xFF00FF21, 
    /* 634   */ 0xFF00FF20, 
    /* 635   */ 0xFF00FF1E, 
    /* 636   */ 0xFF00FF1D, 
    /* 637   */ 0xFF00FF1B, 
    /* 638   */ 0xFF00FF1A, 
    /* 639   */ 0xFF00FF18, 
    /* 640   */ 0xFF00FF16, 
    /* 641   */ 0xFF00FF15, 
    /* 642   */ 0xFF00FF13, 
    /* 643   */ 0xFF00FF12, 
    /* 644   */ 0xFF00FF10, 
    /* 645   */ 0xFF00FF0F, 
    /* 646   */ 0xFF00FF0D, 
    /* 647   */ 0xFF00FF0C, 
    /* 648   */ 0xFF00FF0A, 
    /* 649   */ 0xFF00FF09, 
    /* 650   */ 0xFF00FF07, 
    /* 651   */ 0xFF00FF06, 
    /* 652   */ 0xFF00FF04, 
    /* 653   */ 0xFF00FF03, 
    /* 654   */ 0xFF00FF01, 
    /* 655   */ 0xFF00FF00, 
    /* 656   */ 0xFF01FF00, 
    /* 657   */ 0xFF03FF00, 
    /* 658   */ 0xFF04FF00, 
    /* 659   */ 0xFF06FF00, 
    /* 660   */ 0xFF07FF00, 
    /* 661   */ 0xFF09FF00, 
    /* 662   */ 0xFF0AFF00, 
    /* 663   */ 0xFF0CFF00, 
    /* 664   */ 0xFF0DFF00, 
    /* 665   */ 0xFF0FFF00, 
    /* 666   */ 0xFF10FF00, 
    /* 667   */ 0xFF12FF00, 
    /* 668   */ 0xFF13FF00, 
    /* 669   */ 0xFF15FF00, 
    /* 670   */ 0xFF16FF00, 
    /* 671   */ 0xFF18FF00, 
    /* 672   */ 0xFF1AFF00, 
    /* 673   */ 0xFF1BFF00, 
    /* 674   */ 0xFF1DFF00, 
    /* 675   */ 0xFF1EFF00, 
    /* 676   */ 0xFF20FF00, 
    /* 677   */ 0xFF21FF00, 
    /* 678   */ 0xFF23FF00, 
    /* 679   */ 0xFF24FF00, 
    /* 680   */ 0xFF26FF00, 
    /* 681   */ 0xFF27FF00, 
    /* 682   */ 0xFF29FF00, 
    /* 683   */ 0xFF2AFF00, 
    /* 684   */ 0xFF2CFF00, 
    /* 685   */ 0xFF2DFF00, 
    /* 686   */ 0xFF2FFF00, 
    /* 687   */ 0xFF30FF00, 
    /* 688   */ 0xFF32FF00, 
    /* 689   */ 0xFF34FF00, 
    /* 690   */ 0xFF35FF00, 
    /* 691   */ 0xFF37FF00, 
    /* 692   */ 0xFF38FF00, 
    /* 693   */ 0xFF3AFF00, 
    /* 694   */ 0xFF3BFF00, 
    /* 695   */ 0xFF3DFF00, 
    /* 696   */ 0xFF3EFF00, 
    /* 697   */ 0xFF40FF00, 
    /* 698   */ 0xFF41FF00, 
    /* 699   */ 0xFF43FF00, 
    /* 700   */ 0xFF44FF00, 
    /* 701   */ 0xFF46FF00, 
    /* 702   */ 0xFF47FF00, 
    /* 703   */ 0xFF49FF00, 
    /* 704   */ 0xFF4AFF00, 
    /* 705   */ 0xFF4CFF00, 
    /* 706   */ 0xFF4EFF00, 
    /* 707   */ 0xFF4FFF00, 
    /* 708   */ 0xFF51FF00, 
    /* 709   */ 0xFF52FF00, 
    /* 710   */ 0xFF54FF00, 
    /* 711   */ 0xFF55FF00, 
    /* 712   */ 0xFF57FF00, 
    /* 713   */ 0xFF58FF00, 
    /* 714   */ 0xFF5AFF00, 
    /* 715   */ 0xFF5BFF00, 
    /* 716   */ 0xFF5DFF00, 
    /* 717   */ 0xFF5EFF00, 
    /* 718   */ 0xFF60FF00, 
    /* 719   */ 0xFF61FF00, 
    /* 720   */ 0xFF63FF00, 
    /* 721   */ 0xFF64FF00, 
    /* 722   */ 0xFF66FF00, 
    /* 723   */ 0xFF68FF00, 
    /* 724   */ 0xFF69FF00, 
    /* 725   */ 0xFF6BFF00, 
    /* 726   */ 0xFF6CFF00, 
    /* 727   */ 0xFF6EFF00, 
    /* 728   */ 0xFF6FFF00, 
    /* 729   */ 0xFF71FF00, 
    /* 730   */ 0xFF72FF00, 
    /* 731   */ 0xFF74FF00, 
    /* 732   */ 0xFF75FF00, 
    /* 733   */ 0xFF77FF00, 
    /* 734   */ 0xFF78FF00, 
    /* 735   */ 0xFF7AFF00, 
    /* 736   */ 0xFF7BFF00, 
    /* 737   */ 0xFF7DFF00, 
    /* 738   */ 0xFF7EFF00, 
    /* 739   */ 0xFF80FF00, 
    /* 740   */ 0xFF82FF00, 
    /* 741   */ 0xFF83FF00, 
    /* 742   */ 0xFF85FF00, 
    /* 743   */ 0xFF86FF00, 
    /* 744   */ 0xFF88FF00, 
    /* 745   */ 0xFF89FF00, 
    /* 746   */ 0xFF8BFF00, 
    /* 747   */ 0xFF8CFF00, 
    /* 748   */ 0xFF8EFF00, 
    /* 749   */ 0xFF8FFF00, 
    /* 750   */ 0xFF91FF00, 
    /* 751   */ 0xFF92FF00, 
    /* 752   */ 0xFF94FF00, 
    /* 753   */ 0xFF95FF00, 
    /* 754   */ 0xFF97FF00, 
    /* 755   */ 0xFF98FF00, 
    /* 756   */ 0xFF9AFF00, 
    /* 757   */ 0xFF9CFF00, 
    /* 758   */ 0xFF9DFF00, 
    /* 759   */ 0xFF9FFF00, 
    /* 760   */ 0xFFA0FF00, 
    /* 761   */ 0xFFA2FF00, 
    /* 762   */ 0xFFA3FF00, 
    /* 763   */ 0xFFA5FF00, 
    /* 764   */ 0xFFA6FF00, 
    /* 765   */ 0xFFA8FF00, 
    /* 766   */ 0xFFA9FF00, 
    /* 767   */ 0xFFABFF00, 
    /* 768   */ 0xFFACFF00, 
    /* 769   */ 0xFFAEFF00, 
    /* 770   */ 0xFFAFFF00, 
    /* 771   */ 0xFFB1FF00, 
    /* 772   */ 0xFFB3FF00, 
    /* 773   */ 0xFFB4FF00, 
    /* 774   */ 0xFFB6FF00, 
    /* 775   */ 0xFFB7FF00, 
    /* 776   */ 0xFFB9FF00, 
    /* 777   */ 0xFFBAFF00, 
    /* 778   */ 0xFFBCFF00, 
    /* 779   */ 0xFFBDFF00, 
    /* 780   */ 0xFFBFFF00, 
    /* 781   */ 0xFFC0FF00, 
    /* 782   */ 0xFFC2FF00, 
    /* 783   */ 0xFFC3FF00, 
    /* 784   */ 0xFFC5FF00, 
    /* 785   */ 0xFFC6FF00, 
    /* 786   */ 0xFFC8FF00, 
    /* 787   */ 0xFFC9FF00, 
    /* 788   */ 0xFFCBFF00, 
    /* 789   */ 0xFFCDFF00, 
    /* 790   */ 0xFFCEFF00, 
    /* 791   */ 0xFFD0FF00, 
    /* 792   */ 0xFFD1FF00, 
    /* 793   */ 0xFFD3FF00, 
    /* 794   */ 0xFFD4FF00, 
    /* 795   */ 0xFFD6FF00, 
    /* 796   */ 0xFFD7FF00, 
    /* 797   */ 0xFFD9FF00, 
    /* 798   */ 0xFFDAFF00, 
    /* 799   */ 0xFFDCFF00, 
    /* 800   */ 0xFFDDFF00, 
    /* 801   */ 0xFFDFFF00, 
    /* 802   */ 0xFFE0FF00, 
    /* 803   */ 0xFFE2FF00, 
    /* 804   */ 0xFFE3FF00, 
    /* 805   */ 0xFFE5FF00, 
    /* 806   */ 0xFFE7FF00, 
    /* 807   */ 0xFFE8FF00, 
    /* 808   */ 0xFFEAFF00, 
    /* 809   */ 0xFFEBFF00, 
    /* 810   */ 0xFFEDFF00, 
    /* 811   */ 0xFFEEFF00, 
    /* 812   */ 0xFFF0FF00, 
    /* 813   */ 0xFFF1FF00, 
    /* 814   */ 0xFFF3FF00, 
    /* 815   */ 0xFFF4FF00, 
    /* 816   */ 0xFFF6FF00, 
    /* 817   */ 0xFFF7FF00, 
    /* 818   */ 0xFFF9FF00, 
    /* 819   */ 0xFFFAFF00, 
    /* 820   */ 0xFFFCFF00, 
    /* 821   */ 0xFFFDFF00, 
    /* 822   */ 0xFFFFFE00, 
    /* 823   */ 0xFFFFFC00, 
    /* 824   */ 0xFFFFFB00, 
    /* 825   */ 0xFFFFF900, 
    /* 826   */ 0xFFFFF800, 
    /* 827   */ 0xFFFFF600, 
    /* 828   */ 0xFFFFF500, 
    /* 829   */ 0xFFFFF300, 
    /* 830   */ 0xFFFFF200, 
    /* 831   */ 0xFFFFF000, 
    /* 832   */ 0xFFFFEF00, 
    /* 833   */ 0xFFFFED00, 
    /* 834   */ 0xFFFFEC00, 
    /* 835   */ 0xFFFFEA00, 
    /* 836   */ 0xFFFFE900, 
    /* 837   */ 0xFFFFE700, 
    /* 838   */ 0xFFFFE600, 
    /* 839   */ 0xFFFFE400, 
    /* 840   */ 0xFFFFE200, 
    /* 841   */ 0xFFFFE100, 
    /* 842   */ 0xFFFFDF00, 
    /* 843   */ 0xFFFFDE00, 
    /* 844   */ 0xFFFFDC00, 
    /* 845   */ 0xFFFFDB00, 
    /* 846   */ 0xFFFFD900, 
    /* 847   */ 0xFFFFD800, 
    /* 848   */ 0xFFFFD600, 
    /* 849   */ 0xFFFFD500, 
    /* 850   */ 0xFFFFD300, 
    /* 851   */ 0xFFFFD200, 
    /* 852   */ 0xFFFFD000, 
    /* 853   */ 0xFFFFCF00, 
    /* 854   */ 0xFFFFCD00, 
    /* 855   */ 0xFFFFCC00, 
    /* 856   */ 0xFFFFCA00, 
    /* 857   */ 0xFFFFC800, 
    /* 858   */ 0xFFFFC700, 
    /* 859   */ 0xFFFFC500, 
    /* 860   */ 0xFFFFC400, 
    /* 861   */ 0xFFFFC200, 
    /* 862   */ 0xFFFFC100, 
    /* 863   */ 0xFFFFBF00, 
    /* 864   */ 0xFFFFBE00, 
    /* 865   */ 0xFFFFBC00, 
    /* 866   */ 0xFFFFBB00, 
    /* 867   */ 0xFFFFB900, 
    /* 868   */ 0xFFFFB800, 
    /* 869   */ 0xFFFFB600, 
    /* 870   */ 0xFFFFB500, 
    /* 871   */ 0xFFFFB300, 
    /* 872   */ 0xFFFFB100, 
    /* 873   */ 0xFFFFB000, 
    /* 874   */ 0xFFFFAE00, 
    /* 875   */ 0xFFFFAD00, 
    /* 876   */ 0xFFFFAB00, 
    /* 877   */ 0xFFFFAA00, 
    /* 878   */ 0xFFFFA800, 
    /* 879   */ 0xFFFFA700, 
    /* 880   */ 0xFFFFA500, 
    /* 881   */ 0xFFFFA400, 
    /* 882   */ 0xFFFFA200, 
    /* 883   */ 0xFFFFA100, 
    /* 884   */ 0xFFFF9F00, 
    /* 885   */ 0xFFFF9E00, 
    /* 886   */ 0xFFFF9C00, 
    /* 887   */ 0xFFFF9B00, 
    /* 888   */ 0xFFFF9900, 
    /* 889   */ 0xFFFF9700, 
    /* 890   */ 0xFFFF9600, 
    /* 891   */ 0xFFFF9400, 
    /* 892   */ 0xFFFF9300, 
    /* 893   */ 0xFFFF9100, 
    /* 894   */ 0xFFFF9000, 
    /* 895   */ 0xFFFF8E00, 
    /* 896   */ 0xFFFF8D00, 
    /* 897   */ 0xFFFF8B00, 
    /* 898   */ 0xFFFF8A00, 
    /* 899   */ 0xFFFF8800, 
    /* 900   */ 0xFFFF8700, 
    /* 901   */ 0xFFFF8500, 
    /* 902   */ 0xFFFF8400, 
    /* 903   */ 0xFFFF8200, 
    /* 904   */ 0xFFFF8100, 
    /* 905   */ 0xFFFF7F00, 
    /* 906   */ 0xFFFF7D00, 
    /* 907   */ 0xFFFF7C00, 
    /* 908   */ 0xFFFF7A00, 
    /* 909   */ 0xFFFF7900, 
    /* 910   */ 0xFFFF7700, 
    /* 911   */ 0xFFFF7600, 
    /* 912   */ 0xFFFF7400, 
    /* 913   */ 0xFFFF7300, 
    /* 914   */ 0xFFFF7100, 
    /* 915   */ 0xFFFF7000, 
    /* 916   */ 0xFFFF6E00, 
    /* 917   */ 0xFFFF6D00, 
    /* 918   */ 0xFFFF6B00, 
    /* 919   */ 0xFFFF6A00, 
    /* 920   */ 0xFFFF6800, 
    /* 921   */ 0xFFFF6700, 
    /* 922   */ 0xFFFF6500, 
    /* 923   */ 0xFFFF6300, 
    /* 924   */ 0xFFFF6200, 
    /* 925   */ 0xFFFF6000, 
    /* 926   */ 0xFFFF5F00, 
    /* 927   */ 0xFFFF5D00, 
    /* 928   */ 0xFFFF5C00, 
    /* 929   */ 0xFFFF5A00, 
    /* 930   */ 0xFFFF5900, 
    /* 931   */ 0xFFFF5700, 
    /* 932   */ 0xFFFF5600, 
    /* 933   */ 0xFFFF5400, 
    /* 934   */ 0xFFFF5300, 
    /* 935   */ 0xFFFF5100, 
    /* 936   */ 0xFFFF5000, 
    /* 937   */ 0xFFFF4E00, 
    /* 938   */ 0xFFFF4D00, 
    /* 939   */ 0xFFFF4B00, 
    /* 940   */ 0xFFFF4900, 
    /* 941   */ 0xFFFF4800, 
    /* 942   */ 0xFFFF4600, 
    /* 943   */ 0xFFFF4500, 
    /* 944   */ 0xFFFF4300, 
    /* 945   */ 0xFFFF4200, 
    /* 946   */ 0xFFFF4000, 
    /* 947   */ 0xFFFF3F00, 
    /* 948   */ 0xFFFF3D00, 
    /* 949   */ 0xFFFF3C00, 
    /* 950   */ 0xFFFF3A00, 
    /* 951   */ 0xFFFF3900, 
    /* 952   */ 0xFFFF3700, 
    /* 953   */ 0xFFFF3600, 
    /* 954   */ 0xFFFF3400, 
    /* 955   */ 0xFFFF3300, 
    /* 956   */ 0xFFFF3100, 
    /* 957   */ 0xFFFF2F00, 
    /* 958   */ 0xFFFF2E00, 
    /* 959   */ 0xFFFF2C00, 
    /* 960   */ 0xFFFF2B00, 
    /* 961   */ 0xFFFF2900, 
    /* 962   */ 0xFFFF2800, 
    /* 963   */ 0xFFFF2600, 
    /* 964   */ 0xFFFF2500, 
    /* 965   */ 0xFFFF2300, 
    /* 966   */ 0xFFFF2200, 
    /* 967   */ 0xFFFF2000, 
    /* 968   */ 0xFFFF1F00, 
    /* 969   */ 0xFFFF1D00, 
    /* 970   */ 0xFFFF1C00, 
    /* 971   */ 0xFFFF1A00, 
    /* 972   */ 0xFFFF1800, 
    /* 973   */ 0xFFFF1700, 
    /* 974   */ 0xFFFF1500, 
    /* 975   */ 0xFFFF1400, 
    /* 976   */ 0xFFFF1200, 
    /* 977   */ 0xFFFF1100, 
    /* 978   */ 0xFFFF0F00, 
    /* 979   */ 0xFFFF0E00, 
    /* 980   */ 0xFFFF0C00, 
    /* 981   */ 0xFFFF0B00, 
    /* 982   */ 0xFFFF0900, 
    /* 983   */ 0xFFFF0800, 
    /* 984   */ 0xFFFF0600, 
    /* 985   */ 0xFFFF0500, 
    /* 986   */ 0xFFFF0300, 
    /* 987   */ 0xFFFF0200, 
    /* 988   */ 0xFFFF0000, 
    /* 989   */ 0xFFFF0001, 
    /* 990   */ 0xFFFF0002, 
    /* 991   */ 0xFFFF0004, 
    /* 992   */ 0xFFFF0005, 
    /* 993   */ 0xFFFF0007, 
    /* 994   */ 0xFFFF0008, 
    /* 995   */ 0xFFFF000A, 
    /* 996   */ 0xFFFF000B, 
    /* 997   */ 0xFFFF000D, 
    /* 998   */ 0xFFFF000E, 
    /* 999   */ 0xFFFF0010, 
    /* 1000  */ 0xFFFF0011, 
    /* 1001  */ 0xFFFF0013, 
    /* 1002  */ 0xFFFF0014, 
    /* 1003  */ 0xFFFF0016, 
    /* 1004  */ 0xFFFF0017, 
    /* 1005  */ 0xFFFF0019, 
    /* 1006  */ 0xFFFF001B, 
    /* 1007  */ 0xFFFF001C, 
    /* 1008  */ 0xFFFF001E, 
    /* 1009  */ 0xFFFF001F, 
    /* 1010  */ 0xFFFF0021, 
    /* 1011  */ 0xFFFF0022, 
    /* 1012  */ 0xFFFF0024, 
    /* 1013  */ 0xFFFF0025, 
    /* 1014  */ 0xFFFF0027, 
    /* 1015  */ 0xFFFF0028, 
    /* 1016  */ 0xFFFF002A, 
    /* 1017  */ 0xFFFF002B, 
    /* 1018  */ 0xFFFF002D, 
    /* 1019  */ 0xFFFF002E, 
    /* 1020  */ 0xFFFF0030, 
    /* 1021  */ 0xFFFF0031, 
    /* 1022  */ 0xFFFF0033, 
    /* 1023  */ 0xFFFF0035, 
    /* 1024  */ 0xFFFF0036, 
    /* 1025  */ 0xFFFF0038, 
    /* 1026  */ 0xFFFF0039, 
    /* 1027  */ 0xFFFF003B, 
    /* 1028  */ 0xFFFF003C, 
    /* 1029  */ 0xFFFF003E, 
    /* 1030  */ 0xFFFF003F, 
    /* 1031  */ 0xFFFF0041, 
    /* 1032  */ 0xFFFF0042, 
    /* 1033  */ 0xFFFF0044, 
    /* 1034  */ 0xFFFF0045, 
    /* 1035  */ 0xFFFF0047, 
    /* 1036  */ 0xFFFF0048, 
    /* 1037  */ 0xFFFF004A, 
    /* 1038  */ 0xFFFF004B, 
    /* 1039  */ 0xFFFF004D, 
    /* 1040  */ 0xFFFF004F, 
    /* 1041  */ 0xFFFF0050, 
    /* 1042  */ 0xFFFF0052, 
    /* 1043  */ 0xFFFF0053, 
    /* 1044  */ 0xFFFF0055, 
    /* 1045  */ 0xFFFF0056, 
    /* 1046  */ 0xFFFF0058, 
    /* 1047  */ 0xFFFF0059, 
    /* 1048  */ 0xFFFF005B, 
    /* 1049  */ 0xFFFF005C, 
    /* 1050  */ 0xFFFF005E, 
    /* 1051  */ 0xFFFF005F, 
    /* 1052  */ 0xFFFF0061, 
    /* 1053  */ 0xFFFF0062, 
    /* 1054  */ 0xFFFF0064, 
    /* 1055  */ 0xFFFF0065, 
    /* 1056  */ 0xFFFF0067, 
    /* 1057  */ 0xFFFF0069, 
    /* 1058  */ 0xFFFF006A, 
    /* 1059  */ 0xFFFF006C, 
    /* 1060  */ 0xFFFF006D, 
    /* 1061  */ 0xFFFF006F, 
    /* 1062  */ 0xFFFF0070, 
    /* 1063  */ 0xFFFF0072, 
    /* 1064  */ 0xFFFF0073, 
    /* 1065  */ 0xFFFF0075, 
    /* 1066  */ 0xFFFF0076, 
    /* 1067  */ 0xFFFF0078, 
    /* 1068  */ 0xFFFF0079, 
    /* 1069  */ 0xFFFF007B, 
    /* 1070  */ 0xFFFF007C, 
    /* 1071  */ 0xFFFF007E, 
    /* 1072  */ 0xFFFF0080, 
    /* 1073  */ 0xFFFF0081, 
    /* 1074  */ 0xFFFF0083, 
    /* 1075  */ 0xFFFF0084, 
    /* 1076  */ 0xFFFF0086, 
    /* 1077  */ 0xFFFF0087, 
    /* 1078  */ 0xFFFF0089, 
    /* 1079  */ 0xFFFF008A, 
    /* 1080  */ 0xFFFF008C, 
    /* 1081  */ 0xFFFF008D, 
    /* 1082  */ 0xFFFF008F, 
    /* 1083  */ 0xFFFF0090, 
    /* 1084  */ 0xFFFF0092, 
    /* 1085  */ 0xFFFF0093, 
    /* 1086  */ 0xFFFF0095, 
    /* 1087  */ 0xFFFF0096, 
    /* 1088  */ 0xFFFF0098, 
    /* 1089  */ 0xFFFF009A, 
    /* 1090  */ 0xFFFF009B, 
    /* 1091  */ 0xFFFF009D, 
    /* 1092  */ 0xFFFF009E, 
    /* 1093  */ 0xFFFF00A0, 
    /* 1094  */ 0xFFFF00A1, 
    /* 1095  */ 0xFFFF00A3, 
    /* 1096  */ 0xFFFF00A4, 
    /* 1097  */ 0xFFFF00A6, 
    /* 1098  */ 0xFFFF00A7, 
    /* 1099  */ 0xFFFF00A9, 
    /* 1100  */ 0xFFFF00AA, 
    /* 1101  */ 0xFFFF00AC, 
    /* 1102  */ 0xFFFF00AD, 
    /* 1103  */ 0xFFFF00AF, 
    /* 1104  */ 0xFFFF00B0, 
    /* 1105  */ 0xFFFF00B2, 
    /* 1106  */ 0xFFFF00B4, 
    /* 1107  */ 0xFFFF00B5, 
    /* 1108  */ 0xFFFF00B7, 
    /* 1109  */ 0xFFFF00B8, 
    /* 1110  */ 0xFFFF00BA, 
    /* 1111  */ 0xFFFF00BB, 
    /* 1112  */ 0xFFFF00BD, 
    /* 1113  */ 0xFFFF00BE, 
    /* 1114  */ 0xFFFF00C0, 
    /* 1115  */ 0xFFFF00C1, 
    /* 1116  */ 0xFFFF00C3, 
    /* 1117  */ 0xFFFF00C4, 
    /* 1118  */ 0xFFFF00C6, 
    /* 1119  */ 0xFFFF00C7, 
    /* 1120  */ 0xFFFF00C9, 
    /* 1121  */ 0xFFFF00CA, 
    /* 1122  */ 0xFFFF00CC, 
    /* 1123  */ 0xFFFF00CE, 
    /* 1124  */ 0xFFFF00CF, 
    /* 1125  */ 0xFFFF00D1, 
    /* 1126  */ 0xFFFF00D2, 
    /* 1127  */ 0xFFFF00D4, 
    /* 1128  */ 0xFFFF00D5, 
    /* 1129  */ 0xFFFF00D7, 
    /* 1130  */ 0xFFFF00D8, 
    /* 1131  */ 0xFFFF00DA, 
    /* 1132  */ 0xFFFF00DB, 
    /* 1133  */ 0xFFFF00DD, 
    /* 1134  */ 0xFFFF00DE, 
    /* 1135  */ 0xFFFF00E0, 
    /* 1136  */ 0xFFFF00E1, 
    /* 1137  */ 0xFFFF00E3, 
    /* 1138  */ 0xFFFF00E4, 
    /* 1139  */ 0xFFFF00E6, 
    /* 1140  */ 0xFFFF00E8, 
    /* 1141  */ 0xFFFF00E9, 
    /* 1142  */ 0xFFFF00EB, 
    /* 1143  */ 0xFFFF00EC, 
    /* 1144  */ 0xFFFF00EE, 
    /* 1145  */ 0xFFFF00EF, 
    /* 1146  */ 0xFFFF00F1, 
    /* 1147  */ 0xFFFF00F2, 
    /* 1148  */ 0xFFFF00F4, 
    /* 1149  */ 0xFFFF00F5, 
    /* 1150  */ 0xFFFF00F7, 
    /* 1151  */ 0xFFFF00F8, 
    /* 1152  */ 0xFFFF00FA, 
    /* 1153  */ 0xFFFF00FB, 
    /* 1154  */ 0xFFFF00FD, 
    /* 1155  */ 0xFFFFFF00, 
    /* 1156  */ 0xFFFDFF00, 
    /* 1157  */ 0xFFFBFF00, 
    /* 1158  */ 0xFFFAFF00, 
    /* 1159  */ 0xFFF8FF00, 
    /* 1160  */ 0xFFF7FF00, 
    /* 1161  */ 0xFFF5FF00, 
    /* 1162  */ 0xFFF4FF00, 
    /* 1163  */ 0xFFF2FF00, 
    /* 1164  */ 0xFFF1FF00, 
    /* 1165  */ 0xFFEFFF00, 
    /* 1166  */ 0xFFEEFF00, 
    /* 1167  */ 0xFFECFF00, 
    /* 1168  */ 0xFFEBFF00, 
    /* 1169  */ 0xFFE9FF00, 
    /* 1170  */ 0xFFE8FF00, 
    /* 1171  */ 0xFFE6FF00, 
    /* 1172  */ 0xFFE4FF00, 
    /* 1173  */ 0xFFE3FF00, 
    /* 1174  */ 0xFFE1FF00, 
    /* 1175  */ 0xFFE0FF00, 
    /* 1176  */ 0xFFDEFF00, 
    /* 1177  */ 0xFFDDFF00, 
    /* 1178  */ 0xFFDBFF00, 
    /* 1179  */ 0xFFDAFF00, 
    /* 1180  */ 0xFFD8FF00, 
    /* 1181  */ 0xFFD7FF00, 
    /* 1182  */ 0xFFD5FF00, 
    /* 1183  */ 0xFFD4FF00, 
    /* 1184  */ 0xFFD2FF00, 
    /* 1185  */ 0xFFD1FF00, 
    /* 1186  */ 0xFFCFFF00, 
    /* 1187  */ 0xFFCEFF00, 
    /* 1188  */ 0xFFCCFF00, 
    /* 1189  */ 0xFFCAFF00, 
    /* 1190  */ 0xFFC9FF00, 
    /* 1191  */ 0xFFC7FF00, 
    /* 1192  */ 0xFFC6FF00, 
    /* 1193  */ 0xFFC4FF00, 
    /* 1194  */ 0xFFC3FF00, 
    /* 1195  */ 0xFFC1FF00, 
    /* 1196  */ 0xFFC0FF00, 
    /* 1197  */ 0xFFBEFF00, 
    /* 1198  */ 0xFFBDFF00, 
    /* 1199  */ 0xFFBBFF00, 
    /* 1200  */ 0xFFBAFF00, 
    /* 1201  */ 0xFFB8FF00, 
    /* 1202  */ 0xFFB7FF00, 
    /* 1203  */ 0xFFB5FF00, 
    /* 1204  */ 0xFFB4FF00, 
    /* 1205  */ 0xFFB2FF00, 
    /* 1206  */ 0xFFB0FF00, 
    /* 1207  */ 0xFFAFFF00, 
    /* 1208  */ 0xFFADFF00, 
    /* 1209  */ 0xFFACFF00, 
    /* 1210  */ 0xFFAAFF00, 
    /* 1211  */ 0xFFA9FF00, 
    /* 1212  */ 0xFFA7FF00, 
    /* 1213  */ 0xFFA6FF00, 
    /* 1214  */ 0xFFA4FF00, 
    /* 1215  */ 0xFFA3FF00, 
    /* 1216  */ 0xFFA1FF00, 
    /* 1217  */ 0xFFA0FF00, 
    /* 1218  */ 0xFF9EFF00, 
    /* 1219  */ 0xFF9DFF00, 
    /* 1220  */ 0xFF9BFF00, 
    /* 1221  */ 0xFF9AFF00, 
    /* 1222  */ 0xFF98FF00, 
    /* 1223  */ 0xFF96FF00, 
    /* 1224  */ 0xFF95FF00, 
    /* 1225  */ 0xFF93FF00, 
    /* 1226  */ 0xFF92FF00, 
    /* 1227  */ 0xFF90FF00, 
    /* 1228  */ 0xFF8FFF00, 
    /* 1229  */ 0xFF8DFF00, 
    /* 1230  */ 0xFF8CFF00, 
    /* 1231  */ 0xFF8AFF00, 
    /* 1232  */ 0xFF89FF00, 
    /* 1233  */ 0xFF87FF00, 
    /* 1234  */ 0xFF86FF00, 
    /* 1235  */ 0xFF84FF00, 
    /* 1236  */ 0xFF83FF00, 
    /* 1237  */ 0xFF81FF00, 
    /* 1238  */ 0xFF80FF00, 
    /* 1239  */ 0xFF7EFF00, 
    /* 1240  */ 0xFF7CFF00, 
    /* 1241  */ 0xFF7BFF00, 
    /* 1242  */ 0xFF79FF00, 
    /* 1243  */ 0xFF78FF00, 
    /* 1244  */ 0xFF76FF00, 
    /* 1245  */ 0xFF75FF00, 
    /* 1246  */ 0xFF73FF00, 
    /* 1247  */ 0xFF72FF00, 
    /* 1248  */ 0xFF70FF00, 
    /* 1249  */ 0xFF6FFF00, 
    /* 1250  */ 0xFF6DFF00, 
    /* 1251  */ 0xFF6CFF00, 
    /* 1252  */ 0xFF6AFF00, 
    /* 1253  */ 0xFF69FF00, 
    /* 1254  */ 0xFF67FF00, 
    /* 1255  */ 0xFF66FF00, 
    /* 1256  */ 0xFF64FF00, 
    /* 1257  */ 0xFF62FF00, 
    /* 1258  */ 0xFF61FF00, 
    /* 1259  */ 0xFF5FFF00, 
    /* 1260  */ 0xFF5EFF00, 
    /* 1261  */ 0xFF5CFF00, 
    /* 1262  */ 0xFF5BFF00, 
    /* 1263  */ 0xFF59FF00, 
    /* 1264  */ 0xFF58FF00, 
    /* 1265  */ 0xFF56FF00, 
    /* 1266  */ 0xFF55FF00, 
    /* 1267  */ 0xFF53FF00, 
    /* 1268  */ 0xFF52FF00, 
    /* 1269  */ 0xFF50FF00, 
    /* 1270  */ 0xFF4FFF00, 
    /* 1271  */ 0xFF4DFF00, 
    /* 1272  */ 0xFF4BFF00, 
    /* 1273  */ 0xFF4AFF00, 
    /* 1274  */ 0xFF48FF00, 
    /* 1275  */ 0xFF47FF00, 
    /* 1276  */ 0xFF45FF00, 
    /* 1277  */ 0xFF44FF00, 
    /* 1278  */ 0xFF42FF00, 
    /* 1279  */ 0xFF41FF00, 
    /* 1280  */ 0xFF3FFF00, 
    /* 1281  */ 0xFF3EFF00, 
    /* 1282  */ 0xFF3CFF00, 
    /* 1283  */ 0xFF3BFF00, 
    /* 1284  */ 0xFF39FF00, 
    /* 1285  */ 0xFF38FF00, 
    /* 1286  */ 0xFF36FF00, 
    /* 1287  */ 0xFF35FF00, 
    /* 1288  */ 0xFF33FF00, 
    /* 1289  */ 0xFF31FF00, 
    /* 1290  */ 0xFF30FF00, 
    /* 1291  */ 0xFF2EFF00, 
    /* 1292  */ 0xFF2DFF00, 
    /* 1293  */ 0xFF2BFF00, 
    /* 1294  */ 0xFF2AFF00, 
    /* 1295  */ 0xFF28FF00, 
    /* 1296  */ 0xFF27FF00, 
    /* 1297  */ 0xFF25FF00, 
    /* 1298  */ 0xFF24FF00, 
    /* 1299  */ 0xFF22FF00, 
    /* 1300  */ 0xFF21FF00, 
    /* 1301  */ 0xFF1FFF00, 
    /* 1302  */ 0xFF1EFF00, 
    /* 1303  */ 0xFF1CFF00, 
    /* 1304  */ 0xFF1BFF00, 
    /* 1305  */ 0xFF19FF00, 
    /* 1306  */ 0xFF17FF00, 
    /* 1307  */ 0xFF16FF00, 
    /* 1308  */ 0xFF14FF00, 
    /* 1309  */ 0xFF13FF00, 
    /* 1310  */ 0xFF11FF00, 
    /* 1311  */ 0xFF10FF00, 
    /* 1312  */ 0xFF0EFF00, 
    /* 1313  */ 0xFF0DFF00, 
    /* 1314  */ 0xFF0BFF00, 
    /* 1315  */ 0xFF0AFF00, 
    /* 1316  */ 0xFF08FF00, 
    /* 1317  */ 0xFF07FF00, 
    /* 1318  */ 0xFF05FF00, 
    /* 1319  */ 0xFF04FF00, 
    /* 1320  */ 0xFF02FF00, 
    /* 1321  */ 0xFF01FF00, 
    /* 1322  */ 0xFF00FF00, 
    /* 1323  */ 0xFF00FF02, 
    /* 1324  */ 0xFF00FF03, 
    /* 1325  */ 0xFF00FF05, 
    /* 1326  */ 0xFF00FF06, 
    /* 1327  */ 0xFF00FF08, 
    /* 1328  */ 0xFF00FF09, 
    /* 1329  */ 0xFF00FF0B, 
    /* 1330  */ 0xFF00FF0C, 
    /* 1331  */ 0xFF00FF0E, 
    /* 1332  */ 0xFF00FF0F, 
    /* 1333  */ 0xFF00FF11, 
    /* 1334  */ 0xFF00FF12, 
    /* 1335  */ 0xFF00FF14, 
    /* 1336  */ 0xFF00FF15, 
    /* 1337  */ 0xFF00FF17, 
    /* 1338  */ 0xFF00FF18, 
    /* 1339  */ 0xFF00FF1A, 
    /* 1340  */ 0xFF00FF1C, 
    /* 1341  */ 0xFF00FF1D, 
    /* 1342  */ 0xFF00FF1F, 
    /* 1343  */ 0xFF00FF20, 
    /* 1344  */ 0xFF00FF22, 
    /* 1345  */ 0xFF00FF23, 
    /* 1346  */ 0xFF00FF25, 
    /* 1347  */ 0xFF00FF26, 
    /* 1348  */ 0xFF00FF28, 
    /* 1349  */ 0xFF00FF29, 
    /* 1350  */ 0xFF00FF2B, 
    /* 1351  */ 0xFF00FF2C, 
    /* 1352  */ 0xFF00FF2E, 
    /* 1353  */ 0xFF00FF2F, 
    /* 1354  */ 0xFF00FF31, 
    /* 1355  */ 0xFF00FF32, 
    /* 1356  */ 0xFF00FF34, 
    /* 1357  */ 0xFF00FF36, 
    /* 1358  */ 0xFF00FF37, 
    /* 1359  */ 0xFF00FF39, 
    /* 1360  */ 0xFF00FF3A, 
    /* 1361  */ 0xFF00FF3C, 
    /* 1362  */ 0xFF00FF3D, 
    /* 1363  */ 0xFF00FF3F, 
    /* 1364  */ 0xFF00FF40, 
    /* 1365  */ 0xFF00FF42, 
    /* 1366  */ 0xFF00FF43, 
    /* 1367  */ 0xFF00FF45, 
    /* 1368  */ 0xFF00FF46, 
    /* 1369  */ 0xFF00FF48, 
    /* 1370  */ 0xFF00FF49, 
    /* 1371  */ 0xFF00FF4B, 
    /* 1372  */ 0xFF00FF4D, 
    /* 1373  */ 0xFF00FF4E, 
    /* 1374  */ 0xFF00FF50, 
    /* 1375  */ 0xFF00FF51, 
    /* 1376  */ 0xFF00FF53, 
    /* 1377  */ 0xFF00FF54, 
    /* 1378  */ 0xFF00FF56, 
    /* 1379  */ 0xFF00FF57, 
    /* 1380  */ 0xFF00FF59, 
    /* 1381  */ 0xFF00FF5A, 
    /* 1382  */ 0xFF00FF5C, 
    /* 1383  */ 0xFF00FF5D, 
    /* 1384  */ 0xFF00FF5F, 
    /* 1385  */ 0xFF00FF60, 
    /* 1386  */ 0xFF00FF62, 
    /* 1387  */ 0xFF00FF63, 
    /* 1388  */ 0xFF00FF65, 
    /* 1389  */ 0xFF00FF67, 
    /* 1390  */ 0xFF00FF68, 
    /* 1391  */ 0xFF00FF6A, 
    /* 1392  */ 0xFF00FF6B, 
    /* 1393  */ 0xFF00FF6D, 
    /* 1394  */ 0xFF00FF6E, 
    /* 1395  */ 0xFF00FF70, 
    /* 1396  */ 0xFF00FF71, 
    /* 1397  */ 0xFF00FF73, 
    /* 1398  */ 0xFF00FF74, 
    /* 1399  */ 0xFF00FF76, 
    /* 1400  */ 0xFF00FF77, 
    /* 1401  */ 0xFF00FF79, 
    /* 1402  */ 0xFF00FF7A, 
    /* 1403  */ 0xFF00FF7C, 
    /* 1404  */ 0xFF00FF7D, 
    /* 1405  */ 0xFF00FF7F, 
    /* 1406  */ 0xFF00FF81, 
    /* 1407  */ 0xFF00FF82, 
    /* 1408  */ 0xFF00FF84, 
    /* 1409  */ 0xFF00FF85, 
    /* 1410  */ 0xFF00FF87, 
    /* 1411  */ 0xFF00FF88, 
    /* 1412  */ 0xFF00FF8A, 
    /* 1413  */ 0xFF00FF8B, 
    /* 1414  */ 0xFF00FF8D, 
    /* 1415  */ 0xFF00FF8E, 
    /* 1416  */ 0xFF00FF90, 
    /* 1417  */ 0xFF00FF91, 
    /* 1418  */ 0xFF00FF93, 
    /* 1419  */ 0xFF00FF94, 
    /* 1420  */ 0xFF00FF96, 
    /* 1421  */ 0xFF00FF97, 
    /* 1422  */ 0xFF00FF99, 
    /* 1423  */ 0xFF00FF9B, 
    /* 1424  */ 0xFF00FF9C, 
    /* 1425  */ 0xFF00FF9E, 
    /* 1426  */ 0xFF00FF9F, 
    /* 1427  */ 0xFF00FFA1, 
    /* 1428  */ 0xFF00FFA2, 
    /* 1429  */ 0xFF00FFA4, 
    /* 1430  */ 0xFF00FFA5, 
    /* 1431  */ 0xFF00FFA7, 
    /* 1432  */ 0xFF00FFA8, 
    /* 1433  */ 0xFF00FFAA, 
    /* 1434  */ 0xFF00FFAB, 
    /* 1435  */ 0xFF00FFAD, 
    /* 1436  */ 0xFF00FFAE, 
    /* 1437  */ 0xFF00FFB0, 
    /* 1438  */ 0xFF00FFB1, 
    /* 1439  */ 0xFF00FFB3, 
    /* 1440  */ 0xFF00FFB5, 
    /* 1441  */ 0xFF00FFB6, 
    /* 1442  */ 0xFF00FFB8, 
    /* 1443  */ 0xFF00FFB9, 
    /* 1444  */ 0xFF00FFBB, 
    /* 1445  */ 0xFF00FFBC, 
    /* 1446  */ 0xFF00FFBE, 
    /* 1447  */ 0xFF00FFBF, 
    /* 1448  */ 0xFF00FFC1, 
    /* 1449  */ 0xFF00FFC2, 
    /* 1450  */ 0xFF00FFC4, 
    /* 1451  */ 0xFF00FFC5, 
    /* 1452  */ 0xFF00FFC7, 
    /* 1453  */ 0xFF00FFC8, 
    /* 1454  */ 0xFF00FFCA, 
    /* 1455  */ 0xFF00FFCB, 
    /* 1456  */ 0xFF00FFCD, 
    /* 1457  */ 0xFF00FFCF, 
    /* 1458  */ 0xFF00FFD0, 
    /* 1459  */ 0xFF00FFD2, 
    /* 1460  */ 0xFF00FFD3, 
    /* 1461  */ 0xFF00FFD5, 
    /* 1462  */ 0xFF00FFD6, 
    /* 1463  */ 0xFF00FFD8, 
    /* 1464  */ 0xFF00FFD9, 
    /* 1465  */ 0xFF00FFDB, 
    /* 1466  */ 0xFF00FFDC, 
    /* 1467  */ 0xFF00FFDE, 
    /* 1468  */ 0xFF00FFDF, 
    /* 1469  */ 0xFF00FFE1, 
    /* 1470  */ 0xFF00FFE2, 
    /* 1471  */ 0xFF00FFE4, 
    /* 1472  */ 0xFF00FFE6, 
    /* 1473  */ 0xFF00FFE7, 
    /* 1474  */ 0xFF00FFE9, 
    /* 1475  */ 0xFF00FFEA, 
    /* 1476  */ 0xFF00FFEC, 
    /* 1477  */ 0xFF00FFED, 
    /* 1478  */ 0xFF00FFEF, 
    /* 1479  */ 0xFF00FFF0, 
    /* 1480  */ 0xFF00FFF2, 
    /* 1481  */ 0xFF00FFF3, 
    /* 1482  */ 0xFF00FFF5, 
    /* 1483  */ 0xFF00FFF6, 
    /* 1484  */ 0xFF00FFF8, 
    /* 1485  */ 0xFF00FFF9, 
    /* 1486  */ 0xFF00FFFB, 
    /* 1487  */ 0xFF00FFFC, 
    /* 1488  */ 0xFF00FFFE, 
    /* 1489  */ 0xFF00FDFF, 
    /* 1490  */ 0xFF00FCFF, 
    /* 1491  */ 0xFF00FAFF, 
    /* 1492  */ 0xFF00F9FF, 
    /* 1493  */ 0xFF00F7FF, 
    /* 1494  */ 0xFF00F6FF, 
    /* 1495  */ 0xFF00F4FF, 
    /* 1496  */ 0xFF00F3FF, 
    /* 1497  */ 0xFF00F1FF, 
    /* 1498  */ 0xFF00F0FF, 
    /* 1499  */ 0xFF00EEFF, 
    /* 1500  */ 0xFF00EDFF, 
    /* 1501  */ 0xFF00EBFF, 
    /* 1502  */ 0xFF00EAFF, 
    /* 1503  */ 0xFF00E8FF, 
    /* 1504  */ 0xFF00E7FF, 
    /* 1505  */ 0xFF00E5FF, 
    /* 1506  */ 0xFF00E3FF, 
    /* 1507  */ 0xFF00E2FF, 
    /* 1508  */ 0xFF00E0FF, 
    /* 1509  */ 0xFF00DFFF, 
    /* 1510  */ 0xFF00DDFF, 
    /* 1511  */ 0xFF00DCFF, 
    /* 1512  */ 0xFF00DAFF, 
    /* 1513  */ 0xFF00D9FF, 
    /* 1514  */ 0xFF00D7FF, 
    /* 1515  */ 0xFF00D6FF, 
    /* 1516  */ 0xFF00D4FF, 
    /* 1517  */ 0xFF00D3FF, 
    /* 1518  */ 0xFF00D1FF, 
    /* 1519  */ 0xFF00D0FF, 
    /* 1520  */ 0xFF00CEFF, 
    /* 1521  */ 0xFF00CDFF, 
    /* 1522  */ 0xFF00CBFF, 
    /* 1523  */ 0xFF00C9FF, 
    /* 1524  */ 0xFF00C8FF, 
    /* 1525  */ 0xFF00C6FF, 
    /* 1526  */ 0xFF00C5FF, 
    /* 1527  */ 0xFF00C3FF, 
    /* 1528  */ 0xFF00C2FF, 
    /* 1529  */ 0xFF00C0FF, 
    /* 1530  */ 0xFF00BFFF, 
    /* 1531  */ 0xFF00BDFF, 
    /* 1532  */ 0xFF00BCFF, 
    /* 1533  */ 0xFF00BAFF, 
    /* 1534  */ 0xFF00B9FF, 
    /* 1535  */ 0xFF00B7FF, 
    /* 1536  */ 0xFF00B6FF, 
    /* 1537  */ 0xFF00B4FF, 
    /* 1538  */ 0xFF00B3FF, 
    /* 1539  */ 0xFF00B1FF, 
    /* 1540  */ 0xFF00AFFF, 
    /* 1541  */ 0xFF00AEFF, 
    /* 1542  */ 0xFF00ACFF, 
    /* 1543  */ 0xFF00ABFF, 
    /* 1544  */ 0xFF00A9FF, 
    /* 1545  */ 0xFF00A8FF, 
    /* 1546  */ 0xFF00A6FF, 
    /* 1547  */ 0xFF00A5FF, 
    /* 1548  */ 0xFF00A3FF, 
    /* 1549  */ 0xFF00A2FF, 
    /* 1550  */ 0xFF00A0FF, 
    /* 1551  */ 0xFF009FFF, 
    /* 1552  */ 0xFF009DFF, 
    /* 1553  */ 0xFF009CFF, 
    /* 1554  */ 0xFF009AFF, 
    /* 1555  */ 0xFF0099FF, 
    /* 1556  */ 0xFF0097FF, 
    /* 1557  */ 0xFF0095FF, 
    /* 1558  */ 0xFF0094FF, 
    /* 1559  */ 0xFF0092FF, 
    /* 1560  */ 0xFF0091FF, 
    /* 1561  */ 0xFF008FFF, 
    /* 1562  */ 0xFF008EFF, 
    /* 1563  */ 0xFF008CFF, 
    /* 1564  */ 0xFF008BFF, 
    /* 1565  */ 0xFF0089FF, 
    /* 1566  */ 0xFF0088FF, 
    /* 1567  */ 0xFF0086FF, 
    /* 1568  */ 0xFF0085FF, 
    /* 1569  */ 0xFF0083FF, 
    /* 1570  */ 0xFF0082FF, 
    /* 1571  */ 0xFF0080FF, 
    /* 1572  */ 0xFF007EFF, 
    /* 1573  */ 0xFF007DFF, 
    /* 1574  */ 0xFF007BFF, 
    /* 1575  */ 0xFF007AFF, 
    /* 1576  */ 0xFF0078FF, 
    /* 1577  */ 0xFF0077FF, 
    /* 1578  */ 0xFF0075FF, 
    /* 1579  */ 0xFF0074FF, 
    /* 1580  */ 0xFF0072FF, 
    /* 1581  */ 0xFF0071FF, 
    /* 1582  */ 0xFF006FFF, 
    /* 1583  */ 0xFF006EFF, 
    /* 1584  */ 0xFF006CFF, 
    /* 1585  */ 0xFF006BFF, 
    /* 1586  */ 0xFF0069FF, 
    /* 1587  */ 0xFF0068FF, 
    /* 1588  */ 0xFF0066FF, 
    /* 1589  */ 0xFF0064FF, 
    /* 1590  */ 0xFF0063FF, 
    /* 1591  */ 0xFF0061FF, 
    /* 1592  */ 0xFF0060FF, 
    /* 1593  */ 0xFF005EFF, 
    /* 1594  */ 0xFF005DFF, 
    /* 1595  */ 0xFF005BFF, 
    /* 1596  */ 0xFF005AFF, 
    /* 1597  */ 0xFF0058FF, 
    /* 1598  */ 0xFF0057FF, 
    /* 1599  */ 0xFF0055FF, 
    /* 1600  */ 0xFF0054FF, 
    /* 1601  */ 0xFF0052FF, 
    /* 1602  */ 0xFF0051FF, 
    /* 1603  */ 0xFF004FFF, 
    /* 1604  */ 0xFF004EFF, 
    /* 1605  */ 0xFF004CFF, 
    /* 1606  */ 0xFF004AFF, 
    /* 1607  */ 0xFF0049FF, 
    /* 1608  */ 0xFF0047FF, 
    /* 1609  */ 0xFF0046FF, 
    /* 1610  */ 0xFF0044FF, 
    /* 1611  */ 0xFF0043FF, 
    /* 1612  */ 0xFF0041FF, 
    /* 1613  */ 0xFF0040FF, 
    /* 1614  */ 0xFF003EFF, 
    /* 1615  */ 0xFF003DFF, 
    /* 1616  */ 0xFF003BFF, 
    /* 1617  */ 0xFF003AFF, 
    /* 1618  */ 0xFF0038FF, 
    /* 1619  */ 0xFF0037FF, 
    /* 1620  */ 0xFF0035FF, 
    /* 1621  */ 0xFF0034FF, 
    /* 1622  */ 0xFF0032FF, 
    /* 1623  */ 0xFF0030FF, 
    /* 1624  */ 0xFF002FFF, 
    /* 1625  */ 0xFF002DFF, 
    /* 1626  */ 0xFF002CFF, 
    /* 1627  */ 0xFF002AFF, 
    /* 1628  */ 0xFF0029FF, 
    /* 1629  */ 0xFF0027FF, 
    /* 1630  */ 0xFF0026FF, 
    /* 1631  */ 0xFF0024FF, 
    /* 1632  */ 0xFF0023FF, 
    /* 1633  */ 0xFF0021FF, 
    /* 1634  */ 0xFF0020FF, 
    /* 1635  */ 0xFF001EFF, 
    /* 1636  */ 0xFF001DFF, 
    /* 1637  */ 0xFF001BFF, 
    /* 1638  */ 0xFF001AFF, 
    /* 1639  */ 0xFF0018FF, 
    /* 1640  */ 0xFF0016FF, 
    /* 1641  */ 0xFF0015FF, 
    /* 1642  */ 0xFF0013FF, 
    /* 1643  */ 0xFF0012FF, 
    /* 1644  */ 0xFF0010FF, 
    /* 1645  */ 0xFF000FFF, 
    /* 1646  */ 0xFF000DFF, 
    /* 1647  */ 0xFF000CFF, 
    /* 1648  */ 0xFF000AFF, 
    /* 1649  */ 0xFF0009FF, 
    /* 1650  */ 0xFF0007FF, 
    /* 1651  */ 0xFF0006FF, 
    /* 1652  */ 0xFF0004FF, 
    /* 1653  */ 0xFF0003FF, 
    /* 1654  */ 0xFF0001FF, 
    /* 1655  */ 0xFF0000FF, 
    /* 1656  */ 0xFF0100FF, 
    /* 1657  */ 0xFF0300FF, 
    /* 1658  */ 0xFF0400FF, 
    /* 1659  */ 0xFF0600FF, 
    /* 1660  */ 0xFF0700FF, 
    /* 1661  */ 0xFF0900FF, 
    /* 1662  */ 0xFF0A00FF, 
    /* 1663  */ 0xFF0C00FF, 
    /* 1664  */ 0xFF0D00FF, 
    /* 1665  */ 0xFF0F00FF, 
    /* 1666  */ 0xFF1000FF, 
    /* 1667  */ 0xFF1200FF, 
    /* 1668  */ 0xFF1300FF, 
    /* 1669  */ 0xFF1500FF, 
    /* 1670  */ 0xFF1600FF, 
    /* 1671  */ 0xFF1800FF, 
    /* 1672  */ 0xFF1A00FF, 
    /* 1673  */ 0xFF1B00FF, 
    /* 1674  */ 0xFF1D00FF, 
    /* 1675  */ 0xFF1E00FF, 
    /* 1676  */ 0xFF2000FF, 
    /* 1677  */ 0xFF2100FF, 
    /* 1678  */ 0xFF2300FF, 
    /* 1679  */ 0xFF2400FF, 
    /* 1680  */ 0xFF2600FF, 
    /* 1681  */ 0xFF2700FF, 
    /* 1682  */ 0xFF2900FF, 
    /* 1683  */ 0xFF2A00FF, 
    /* 1684  */ 0xFF2C00FF, 
    /* 1685  */ 0xFF2D00FF, 
    /* 1686  */ 0xFF2F00FF, 
    /* 1687  */ 0xFF3000FF, 
    /* 1688  */ 0xFF3200FF, 
    /* 1689  */ 0xFF3400FF, 
    /* 1690  */ 0xFF3500FF, 
    /* 1691  */ 0xFF3700FF, 
    /* 1692  */ 0xFF3800FF, 
    /* 1693  */ 0xFF3A00FF, 
    /* 1694  */ 0xFF3B00FF, 
    /* 1695  */ 0xFF3D00FF, 
    /* 1696  */ 0xFF3E00FF, 
    /* 1697  */ 0xFF4000FF, 
    /* 1698  */ 0xFF4100FF, 
    /* 1699  */ 0xFF4300FF, 
    /* 1700  */ 0xFF4400FF, 
    /* 1701  */ 0xFF4600FF, 
    /* 1702  */ 0xFF4700FF, 
    /* 1703  */ 0xFF4900FF, 
    /* 1704  */ 0xFF4A00FF, 
    /* 1705  */ 0xFF4C00FF, 
    /* 1706  */ 0xFF4E00FF, 
    /* 1707  */ 0xFF4F00FF, 
    /* 1708  */ 0xFF5100FF, 
    /* 1709  */ 0xFF5200FF, 
    /* 1710  */ 0xFF5400FF, 
    /* 1711  */ 0xFF5500FF, 
    /* 1712  */ 0xFF5700FF, 
    /* 1713  */ 0xFF5800FF, 
    /* 1714  */ 0xFF5A00FF, 
    /* 1715  */ 0xFF5B00FF, 
    /* 1716  */ 0xFF5D00FF, 
    /* 1717  */ 0xFF5E00FF, 
    /* 1718  */ 0xFF6000FF, 
    /* 1719  */ 0xFF6100FF, 
    /* 1720  */ 0xFF6300FF, 
    /* 1721  */ 0xFF6400FF, 
    /* 1722  */ 0xFF6600FF, 
    /* 1723  */ 0xFF6800FF, 
    /* 1724  */ 0xFF6900FF, 
    /* 1725  */ 0xFF6B00FF, 
    /* 1726  */ 0xFF6C00FF, 
    /* 1727  */ 0xFF6E00FF, 
    /* 1728  */ 0xFF6F00FF, 
    /* 1729  */ 0xFF7100FF, 
    /* 1730  */ 0xFF7200FF, 
    /* 1731  */ 0xFF7400FF, 
    /* 1732  */ 0xFF7500FF, 
    /* 1733  */ 0xFF7700FF, 
    /* 1734  */ 0xFF7800FF, 
    /* 1735  */ 0xFF7A00FF, 
    /* 1736  */ 0xFF7B00FF, 
    /* 1737  */ 0xFF7D00FF, 
    /* 1738  */ 0xFF7E00FF, 
    /* 1739  */ 0xFF8000FF, 
    /* 1740  */ 0xFF8200FF, 
    /* 1741  */ 0xFF8300FF, 
    /* 1742  */ 0xFF8500FF, 
    /* 1743  */ 0xFF8600FF, 
    /* 1744  */ 0xFF8800FF, 
    /* 1745  */ 0xFF8900FF, 
    /* 1746  */ 0xFF8B00FF, 
    /* 1747  */ 0xFF8C00FF, 
    /* 1748  */ 0xFF8E00FF, 
    /* 1749  */ 0xFF8F00FF, 
    /* 1750  */ 0xFF9100FF, 
    /* 1751  */ 0xFF9200FF, 
    /* 1752  */ 0xFF9400FF, 
    /* 1753  */ 0xFF9500FF, 
    /* 1754  */ 0xFF9700FF, 
    /* 1755  */ 0xFF9800FF, 
    /* 1756  */ 0xFF9A00FF, 
    /* 1757  */ 0xFF9C00FF, 
    /* 1758  */ 0xFF9D00FF, 
    /* 1759  */ 0xFF9F00FF, 
    /* 1760  */ 0xFFA000FF, 
    /* 1761  */ 0xFFA200FF, 
    /* 1762  */ 0xFFA300FF, 
    /* 1763  */ 0xFFA500FF, 
    /* 1764  */ 0xFFA600FF, 
    /* 1765  */ 0xFFA800FF, 
    /* 1766  */ 0xFFA900FF, 
    /* 1767  */ 0xFFAB00FF, 
    /* 1768  */ 0xFFAC00FF, 
    /* 1769  */ 0xFFAE00FF, 
    /* 1770  */ 0xFFAF00FF, 
    /* 1771  */ 0xFFB100FF, 
    /* 1772  */ 0xFFB300FF, 
    /* 1773  */ 0xFFB400FF, 
    /* 1774  */ 0xFFB600FF, 
    /* 1775  */ 0xFFB700FF, 
    /* 1776  */ 0xFFB900FF, 
    /* 1777  */ 0xFFBA00FF, 
    /* 1778  */ 0xFFBC00FF, 
    /* 1779  */ 0xFFBD00FF, 
    /* 1780  */ 0xFFBF00FF, 
    /* 1781  */ 0xFFC000FF, 
    /* 1782  */ 0xFFC200FF, 
    /* 1783  */ 0xFFC300FF, 
    /* 1784  */ 0xFFC500FF, 
    /* 1785  */ 0xFFC600FF, 
    /* 1786  */ 0xFFC800FF, 
    /* 1787  */ 0xFFC900FF, 
    /* 1788  */ 0xFFCB00FF, 
    /* 1789  */ 0xFFCD00FF, 
    /* 1790  */ 0xFFCE00FF, 
    /* 1791  */ 0xFFD000FF, 
    /* 1792  */ 0xFFD100FF, 
    /* 1793  */ 0xFFD300FF, 
    /* 1794  */ 0xFFD400FF, 
    /* 1795  */ 0xFFD600FF, 
    /* 1796  */ 0xFFD700FF, 
    /* 1797  */ 0xFFD900FF, 
    /* 1798  */ 0xFFDA00FF, 
    /* 1799  */ 0xFFDC00FF, 
    /* 1800  */ 0xFFDD00FF, 
    /* 1801  */ 0xFFDF00FF, 
    /* 1802  */ 0xFFE000FF, 
    /* 1803  */ 0xFFE200FF, 
    /* 1804  */ 0xFFE300FF, 
    /* 1805  */ 0xFFE500FF, 
    /* 1806  */ 0xFFE700FF, 
    /* 1807  */ 0xFFE800FF, 
    /* 1808  */ 0xFFEA00FF, 
    /* 1809  */ 0xFFEB00FF, 
    /* 1810  */ 0xFFED00FF, 
    /* 1811  */ 0xFFEE00FF, 
    /* 1812  */ 0xFFF000FF, 
    /* 1813  */ 0xFFF100FF, 
    /* 1814  */ 0xFFF300FF, 
    /* 1815  */ 0xFFF400FF, 
    /* 1816  */ 0xFFF600FF, 
    /* 1817  */ 0xFFF700FF, 
    /* 1818  */ 0xFFF900FF, 
    /* 1819  */ 0xFFFA00FF, 
    /* 1820  */ 0xFFFC00FF, 
    /* 1821  */ 0xFFFD00FF, 
    /* 1822  */ 0xFFFF00FE, 
    /* 1823  */ 0xFFFF00FC, 
    /* 1824  */ 0xFFFF00FB, 
    /* 1825  */ 0xFFFF00F9, 
    /* 1826  */ 0xFFFF00F8, 
    /* 1827  */ 0xFFFF00F6, 
    /* 1828  */ 0xFFFF00F5, 
    /* 1829  */ 0xFFFF00F3, 
    /* 1830  */ 0xFFFF00F2, 
    /* 1831  */ 0xFFFF00F0, 
    /* 1832  */ 0xFFFF00EF, 
    /* 1833  */ 0xFFFF00ED, 
    /* 1834  */ 0xFFFF00EC, 
    /* 1835  */ 0xFFFF00EA, 
    /* 1836  */ 0xFFFF00E9, 
    /* 1837  */ 0xFFFF00E7, 
    /* 1838  */ 0xFFFF00E6, 
    /* 1839  */ 0xFFFF00E4, 
    /* 1840  */ 0xFFFF00E2, 
    /* 1841  */ 0xFFFF00E1, 
    /* 1842  */ 0xFFFF00DF, 
    /* 1843  */ 0xFFFF00DE, 
    /* 1844  */ 0xFFFF00DC, 
    /* 1845  */ 0xFFFF00DB, 
    /* 1846  */ 0xFFFF00D9, 
    /* 1847  */ 0xFFFF00D8, 
    /* 1848  */ 0xFFFF00D6, 
    /* 1849  */ 0xFFFF00D5, 
    /* 1850  */ 0xFFFF00D3, 
    /* 1851  */ 0xFFFF00D2, 
    /* 1852  */ 0xFFFF00D0, 
    /* 1853  */ 0xFFFF00CF, 
    /* 1854  */ 0xFFFF00CD, 
    /* 1855  */ 0xFFFF00CC, 
    /* 1856  */ 0xFFFF00CA, 
    /* 1857  */ 0xFFFF00C8, 
    /* 1858  */ 0xFFFF00C7, 
    /* 1859  */ 0xFFFF00C5, 
    /* 1860  */ 0xFFFF00C4, 
    /* 1861  */ 0xFFFF00C2, 
    /* 1862  */ 0xFFFF00C1, 
    /* 1863  */ 0xFFFF00BF, 
    /* 1864  */ 0xFFFF00BE, 
    /* 1865  */ 0xFFFF00BC, 
    /* 1866  */ 0xFFFF00BB, 
    /* 1867  */ 0xFFFF00B9, 
    /* 1868  */ 0xFFFF00B8, 
    /* 1869  */ 0xFFFF00B6, 
    /* 1870  */ 0xFFFF00B5, 
    /* 1871  */ 0xFFFF00B3, 
    /* 1872  */ 0xFFFF00B1, 
    /* 1873  */ 0xFFFF00B0, 
    /* 1874  */ 0xFFFF00AE, 
    /* 1875  */ 0xFFFF00AD, 
    /* 1876  */ 0xFFFF00AB, 
    /* 1877  */ 0xFFFF00AA, 
    /* 1878  */ 0xFFFF00A8, 
    /* 1879  */ 0xFFFF00A7, 
    /* 1880  */ 0xFFFF00A5, 
    /* 1881  */ 0xFFFF00A4, 
    /* 1882  */ 0xFFFF00A2, 
    /* 1883  */ 0xFFFF00A1, 
    /* 1884  */ 0xFFFF009F, 
    /* 1885  */ 0xFFFF009E, 
    /* 1886  */ 0xFFFF009C, 
    /* 1887  */ 0xFFFF009B, 
    /* 1888  */ 0xFFFF0099, 
    /* 1889  */ 0xFFFF0097, 
    /* 1890  */ 0xFFFF0096, 
    /* 1891  */ 0xFFFF0094, 
    /* 1892  */ 0xFFFF0093, 
    /* 1893  */ 0xFFFF0091, 
    /* 1894  */ 0xFFFF0090, 
    /* 1895  */ 0xFFFF008E, 
    /* 1896  */ 0xFFFF008D, 
    /* 1897  */ 0xFFFF008B, 
    /* 1898  */ 0xFFFF008A, 
    /* 1899  */ 0xFFFF0088, 
    /* 1900  */ 0xFFFF0087, 
    /* 1901  */ 0xFFFF0085, 
    /* 1902  */ 0xFFFF0084, 
    /* 1903  */ 0xFFFF0082, 
    /* 1904  */ 0xFFFF0081, 
    /* 1905  */ 0xFFFF007F, 
    /* 1906  */ 0xFFFF007D, 
    /* 1907  */ 0xFFFF007C, 
    /* 1908  */ 0xFFFF007A, 
    /* 1909  */ 0xFFFF0079, 
    /* 1910  */ 0xFFFF0077, 
    /* 1911  */ 0xFFFF0076, 
    /* 1912  */ 0xFFFF0074, 
    /* 1913  */ 0xFFFF0073, 
    /* 1914  */ 0xFFFF0071, 
    /* 1915  */ 0xFFFF0070, 
    /* 1916  */ 0xFFFF006E, 
    /* 1917  */ 0xFFFF006D, 
    /* 1918  */ 0xFFFF006B, 
    /* 1919  */ 0xFFFF006A, 
    /* 1920  */ 0xFFFF0068, 
    /* 1921  */ 0xFFFF0067, 
    /* 1922  */ 0xFFFF0065, 
    /* 1923  */ 0xFFFF0063, 
    /* 1924  */ 0xFFFF0062, 
    /* 1925  */ 0xFFFF0060, 
    /* 1926  */ 0xFFFF005F, 
    /* 1927  */ 0xFFFF005D, 
    /* 1928  */ 0xFFFF005C, 
    /* 1929  */ 0xFFFF005A, 
    /* 1930  */ 0xFFFF0059, 
    /* 1931  */ 0xFFFF0057, 
    /* 1932  */ 0xFFFF0056, 
    /* 1933  */ 0xFFFF0054, 
    /* 1934  */ 0xFFFF0053, 
    /* 1935  */ 0xFFFF0051, 
    /* 1936  */ 0xFFFF0050, 
    /* 1937  */ 0xFFFF004E, 
    /* 1938  */ 0xFFFF004D, 
    /* 1939  */ 0xFFFF004B, 
    /* 1940  */ 0xFFFF0049, 
    /* 1941  */ 0xFFFF0048, 
    /* 1942  */ 0xFFFF0046, 
    /* 1943  */ 0xFFFF0045, 
    /* 1944  */ 0xFFFF0043, 
    /* 1945  */ 0xFFFF0042, 
    /* 1946  */ 0xFFFF0040, 
    /* 1947  */ 0xFFFF003F, 
    /* 1948  */ 0xFFFF003D, 
    /* 1949  */ 0xFFFF003C, 
    /* 1950  */ 0xFFFF003A, 
    /* 1951  */ 0xFFFF0039, 
    /* 1952  */ 0xFFFF0037, 
    /* 1953  */ 0xFFFF0036, 
    /* 1954  */ 0xFFFF0034, 
    /* 1955  */ 0xFFFF0033, 
    /* 1956  */ 0xFFFF0031, 
    /* 1957  */ 0xFFFF002F, 
    /* 1958  */ 0xFFFF002E, 
    /* 1959  */ 0xFFFF002C, 
    /* 1960  */ 0xFFFF002B, 
    /* 1961  */ 0xFFFF0029, 
    /* 1962  */ 0xFFFF0028, 
    /* 1963  */ 0xFFFF0026, 
    /* 1964  */ 0xFFFF0025, 
    /* 1965  */ 0xFFFF0023, 
    /* 1966  */ 0xFFFF0022, 
    /* 1967  */ 0xFFFF0020, 
    /* 1968  */ 0xFFFF001F, 
    /* 1969  */ 0xFFFF001D, 
    /* 1970  */ 0xFFFF001C, 
    /* 1971  */ 0xFFFF001A, 
    /* 1972  */ 0xFFFF0018, 
    /* 1973  */ 0xFFFF0017, 
    /* 1974  */ 0xFFFF0015, 
    /* 1975  */ 0xFFFF0014, 
    /* 1976  */ 0xFFFF0012, 
    /* 1977  */ 0xFFFF0011, 
    /* 1978  */ 0xFFFF000F, 
    /* 1979  */ 0xFFFF000E, 
    /* 1980  */ 0xFFFF000C, 
    /* 1981  */ 0xFFFF000B, 
    /* 1982  */ 0xFFFF0009, 
    /* 1983  */ 0xFFFF0008, 
    /* 1984  */ 0xFFFF0006, 
    /* 1985  */ 0xFFFF0005, 
    /* 1986  */ 0xFFFF0003, 
    /* 1987  */ 0xFFFF0002, 
    /* 1988  */ 0xFFFF0000, 
    /* 1989  */ 0xFFFF0100, 
    /* 1990  */ 0xFFFF0200, 
    /* 1991  */ 0xFFFF0400, 
    /* 1992  */ 0xFFFF0500, 
    /* 1993  */ 0xFFFF0700, 
    /* 1994  */ 0xFFFF0800, 
    /* 1995  */ 0xFFFF0A00, 
    /* 1996  */ 0xFFFF0B00, 
    /* 1997  */ 0xFFFF0D00, 
    /* 1998  */ 0xFFFF0E00, 
    /* 1999  */ 0xFFFF1000, 
    /* 2000  */ 0xFFFF1100, 
    /* 2001  */ 0xFFFF1300, 
    /* 2002  */ 0xFFFF1400, 
    /* 2003  */ 0xFFFF1600, 
    /* 2004  */ 0xFFFF1700, 
    /* 2005  */ 0xFFFF1900, 
    /* 2006  */ 0xFFFF1B00, 
    /* 2007  */ 0xFFFF1C00, 
    /* 2008  */ 0xFFFF1E00, 
    /* 2009  */ 0xFFFF1F00, 
    /* 2010  */ 0xFFFF2100, 
    /* 2011  */ 0xFFFF2200, 
    /* 2012  */ 0xFFFF2400, 
    /* 2013  */ 0xFFFF2500, 
    /* 2014  */ 0xFFFF2700, 
    /* 2015  */ 0xFFFF2800, 
    /* 2016  */ 0xFFFF2A00, 
    /* 2017  */ 0xFFFF2B00, 
    /* 2018  */ 0xFFFF2D00, 
    /* 2019  */ 0xFFFF2E00, 
    /* 2020  */ 0xFFFF3000, 
    /* 2021  */ 0xFFFF3100, 
    /* 2022  */ 0xFFFF3300, 
    /* 2023  */ 0xFFFF3500, 
    /* 2024  */ 0xFFFF3600, 
    /* 2025  */ 0xFFFF3800, 
    /* 2026  */ 0xFFFF3900, 
    /* 2027  */ 0xFFFF3B00, 
    /* 2028  */ 0xFFFF3C00, 
    /* 2029  */ 0xFFFF3E00, 
    /* 2030  */ 0xFFFF3F00, 
    /* 2031  */ 0xFFFF4100, 
    /* 2032  */ 0xFFFF4200, 
    /* 2033  */ 0xFFFF4400, 
    /* 2034  */ 0xFFFF4500, 
    /* 2035  */ 0xFFFF4700, 
    /* 2036  */ 0xFFFF4800, 
    /* 2037  */ 0xFFFF4A00, 
    /* 2038  */ 0xFFFF4B00, 
    /* 2039  */ 0xFFFF4D00, 
    /* 2040  */ 0xFFFF4F00, 
    /* 2041  */ 0xFFFF5000, 
    /* 2042  */ 0xFFFF5200, 
    /* 2043  */ 0xFFFF5300, 
    /* 2044  */ 0xFFFF5500, 
    /* 2045  */ 0xFFFF5600, 
    /* 2046  */ 0xFFFF5800, 
    /* 2047  */ 0xFFFF5900, 
    /* 2048  */ 0xFFFF5B00, 
    /* 2049  */ 0xFFFF5C00, 
    /* 2050  */ 0xFFFF5E00, 
    /* 2051  */ 0xFFFF5F00, 
    /* 2052  */ 0xFFFF6100, 
    /* 2053  */ 0xFFFF6200, 
    /* 2054  */ 0xFFFF6400, 
    /* 2055  */ 0xFFFF6500, 
    /* 2056  */ 0xFFFF6700, 
    /* 2057  */ 0xFFFF6900, 
    /* 2058  */ 0xFFFF6A00, 
    /* 2059  */ 0xFFFF6C00, 
    /* 2060  */ 0xFFFF6D00, 
    /* 2061  */ 0xFFFF6F00, 
    /* 2062  */ 0xFFFF7000, 
    /* 2063  */ 0xFFFF7200, 
    /* 2064  */ 0xFFFF7300, 
    /* 2065  */ 0xFFFF7500, 
    /* 2066  */ 0xFFFF7600, 
    /* 2067  */ 0xFFFF7800, 
    /* 2068  */ 0xFFFF7900, 
    /* 2069  */ 0xFFFF7B00, 
    /* 2070  */ 0xFFFF7C00, 
    /* 2071  */ 0xFFFF7E00, 
    /* 2072  */ 0xFFFF8000, 
    /* 2073  */ 0xFFFF8100, 
    /* 2074  */ 0xFFFF8300, 
    /* 2075  */ 0xFFFF8400, 
    /* 2076  */ 0xFFFF8600, 
    /* 2077  */ 0xFFFF8700, 
    /* 2078  */ 0xFFFF8900, 
    /* 2079  */ 0xFFFF8A00, 
    /* 2080  */ 0xFFFF8C00, 
    /* 2081  */ 0xFFFF8D00, 
    /* 2082  */ 0xFFFF8F00, 
    /* 2083  */ 0xFFFF9000, 
    /* 2084  */ 0xFFFF9200, 
    /* 2085  */ 0xFFFF9300, 
    /* 2086  */ 0xFFFF9500, 
    /* 2087  */ 0xFFFF9600, 
    /* 2088  */ 0xFFFF9800, 
    /* 2089  */ 0xFFFF9A00, 
    /* 2090  */ 0xFFFF9B00, 
    /* 2091  */ 0xFFFF9D00, 
    /* 2092  */ 0xFFFF9E00, 
    /* 2093  */ 0xFFFFA000, 
    /* 2094  */ 0xFFFFA100, 
    /* 2095  */ 0xFFFFA300, 
    /* 2096  */ 0xFFFFA400, 
    /* 2097  */ 0xFFFFA600, 
    /* 2098  */ 0xFFFFA700, 
    /* 2099  */ 0xFFFFA900, 
    /* 2100  */ 0xFFFFAA00, 
    /* 2101  */ 0xFFFFAC00, 
    /* 2102  */ 0xFFFFAD00, 
    /* 2103  */ 0xFFFFAF00, 
    /* 2104  */ 0xFFFFB000, 
    /* 2105  */ 0xFFFFB200, 
    /* 2106  */ 0xFFFFB400, 
    /* 2107  */ 0xFFFFB500, 
    /* 2108  */ 0xFFFFB700, 
    /* 2109  */ 0xFFFFB800, 
    /* 2110  */ 0xFFFFBA00, 
    /* 2111  */ 0xFFFFBB00, 
    /* 2112  */ 0xFFFFBD00, 
    /* 2113  */ 0xFFFFBE00, 
    /* 2114  */ 0xFFFFC000, 
    /* 2115  */ 0xFFFFC100, 
    /* 2116  */ 0xFFFFC300, 
    /* 2117  */ 0xFFFFC400, 
    /* 2118  */ 0xFFFFC600, 
    /* 2119  */ 0xFFFFC700, 
    /* 2120  */ 0xFFFFC900, 
    /* 2121  */ 0xFFFFCA00, 
    /* 2122  */ 0xFFFFCC00, 
    /* 2123  */ 0xFFFFCE00, 
    /* 2124  */ 0xFFFFCF00, 
    /* 2125  */ 0xFFFFD100, 
    /* 2126  */ 0xFFFFD200, 
    /* 2127  */ 0xFFFFD400, 
    /* 2128  */ 0xFFFFD500, 
    /* 2129  */ 0xFFFFD700, 
    /* 2130  */ 0xFFFFD800, 
    /* 2131  */ 0xFFFFDA00, 
    /* 2132  */ 0xFFFFDB00, 
    /* 2133  */ 0xFFFFDD00, 
    /* 2134  */ 0xFFFFDE00, 
    /* 2135  */ 0xFFFFE000, 
    /* 2136  */ 0xFFFFE100, 
    /* 2137  */ 0xFFFFE300, 
    /* 2138  */ 0xFFFFE400, 
    /* 2139  */ 0xFFFFE600, 
    /* 2140  */ 0xFFFFE800, 
    /* 2141  */ 0xFFFFE900, 
    /* 2142  */ 0xFFFFEB00, 
    /* 2143  */ 0xFFFFEC00, 
    /* 2144  */ 0xFFFFEE00, 
    /* 2145  */ 0xFFFFEF00, 
    /* 2146  */ 0xFFFFF100, 
    /* 2147  */ 0xFFFFF200, 
    /* 2148  */ 0xFFFFF400, 
    /* 2149  */ 0xFFFFF500, 
    /* 2150  */ 0xFFFFF700, 
    /* 2151  */ 0xFFFFF800, 
    /* 2152  */ 0xFFFFFA00, 
    /* 2153  */ 0xFFFFFB00, 
    /* 2154  */ 0xFFFFFD00, 
    /* 2155  */ 0xFFFFFF00, 
    /* 2156  */ 0xFFFBFB03, 
    /* 2157  */ 0xFFF8F806, 
    /* 2158  */ 0xFFF5F509, 
    /* 2159  */ 0xFFF2F20C, 
    /* 2160  */ 0xFFEFEF0F, 
    /* 2161  */ 0xFFECEC12, 
    /* 2162  */ 0xFFE9E915, 
    /* 2163  */ 0xFFE6E618, 
    /* 2164  */ 0xFFE3E31B, 
    /* 2165  */ 0xFFE0E01E, 
    /* 2166  */ 0xFFDDDD21, 
    /* 2167  */ 0xFFDADA24, 
    /* 2168  */ 0xFFD7D727, 
    /* 2169  */ 0xFFD4D42A, 
    /* 2170  */ 0xFFD1D12D, 
    /* 2171  */ 0xFFCECE30, 
    /* 2172  */ 0xFFCACA34, 
    /* 2173  */ 0xFFC7C737, 
    /* 2174  */ 0xFFC4C43A, 
    /* 2175  */ 0xFFC1C13D, 
    /* 2176  */ 0xFFBEBE40, 
    /* 2177  */ 0xFFBBBB43, 
    /* 2178  */ 0xFFB8B846, 
    /* 2179  */ 0xFFB5B549, 
    /* 2180  */ 0xFFB2B24C, 
    /* 2181  */ 0xFFAFAF4F, 
    /* 2182  */ 0xFFACAC52, 
    /* 2183  */ 0xFFA9A955, 
    /* 2184  */ 0xFFA6A658, 
    /* 2185  */ 0xFFA3A35B, 
    /* 2186  */ 0xFFA0A05E, 
    /* 2187  */ 0xFF9D9D61, 
    /* 2188  */ 0xFF9A9A64, 
    /* 2189  */ 0xFF969668, 
    /* 2190  */ 0xFF93936B, 
    /* 2191  */ 0xFF90906E, 
    /* 2192  */ 0xFF8D8D71, 
    /* 2193  */ 0xFF8A8A74, 
    /* 2194  */ 0xFF878777, 
    /* 2195  */ 0xFF84847A, 
    /* 2196  */ 0xFF81817D, 
    /* 2197  */ 0xFF7E7E80, 
    /* 2198  */ 0xFF7B7B83, 
    /* 2199  */ 0xFF787886, 
    /* 2200  */ 0xFF757589, 
    /* 2201  */ 0xFF72728C, 
    /* 2202  */ 0xFF6F6F8F, 
    /* 2203  */ 0xFF6C6C92, 
    /* 2204  */ 0xFF696995, 
    /* 2205  */ 0xFF666698, 
    /* 2206  */ 0xFF62629C, 
    /* 2207  */ 0xFF5F5F9F, 
    /* 2208  */ 0xFF5C5CA2, 
    /* 2209  */ 0xFF5959A5, 
    /* 2210  */ 0xFF5656A8, 
    /* 2211  */ 0xFF5353AB, 
    /* 2212  */ 0xFF5050AE, 
    /* 2213  */ 0xFF4D4DB1, 
    /* 2214  */ 0xFF4A4AB4, 
    /* 2215  */ 0xFF4747B7, 
    /* 2216  */ 0xFF4444BA, 
    /* 2217  */ 0xFF4141BD, 
    /* 2218  */ 0xFF3E3EC0, 
    /* 2219  */ 0xFF3B3BC3, 
    /* 2220  */ 0xFF3838C6, 
    /* 2221  */ 0xFF3535C9, 
    /* 2222  */ 0xFF3131CD, 
    /* 2223  */ 0xFF2E2ED0, 
    /* 2224  */ 0xFF2B2BD3, 
    /* 2225  */ 0xFF2828D6, 
    /* 2226  */ 0xFF2525D9, 
    /* 2227  */ 0xFF2222DC, 
    /* 2228  */ 0xFF1F1FDF, 
    /* 2229  */ 0xFF1C1CE2, 
    /* 2230  */ 0xFF1919E5, 
    /* 2231  */ 0xFF1616E8, 
    /* 2232  */ 0xFF1313EB, 
    /* 2233  */ 0xFF1010EE, 
    /* 2234  */ 0xFF0D0DF1, 
    /* 2235  */ 0xFF0A0AF4, 
    /* 2236  */ 0xFF0707F7, 
    /* 2237  */ 0xFF0404FA, 
    /* 2238  */ 0xFF0101FD, 
    /* 2239  */ 0xFF0200FC, 
    /* 2240  */ 0xFF0500F9, 
    /* 2241  */ 0xFF0800F6, 
    /* 2242  */ 0xFF0B00F3, 
    /* 2243  */ 0xFF0E00F0, 
    /* 2244  */ 0xFF1100ED, 
    /* 2245  */ 0xFF1400EA, 
    /* 2246  */ 0xFF1700E7, 
    /* 2247  */ 0xFF1A00E4, 
    /* 2248  */ 0xFF1D00E1, 
    /* 2249  */ 0xFF2000DE, 
    /* 2250  */ 0xFF2300DB, 
    /* 2251  */ 0xFF2600D8, 
    /* 2252  */ 0xFF2900D5, 
    /* 2253  */ 0xFF2C00D2, 
    /* 2254  */ 0xFF2F00CF, 
    /* 2255  */ 0xFF3200CC, 
    /* 2256  */ 0xFF3600C8, 
    /* 2257  */ 0xFF3900C5, 
    /* 2258  */ 0xFF3C00C2, 
    /* 2259  */ 0xFF3F00BF, 
    /* 2260  */ 0xFF4200BC, 
    /* 2261  */ 0xFF4500B9, 
    /* 2262  */ 0xFF4800B6, 
    /* 2263  */ 0xFF4B00B3, 
    /* 2264  */ 0xFF4E00B0, 
    /* 2265  */ 0xFF5100AD, 
    /* 2266  */ 0xFF5400AA, 
    /* 2267  */ 0xFF5700A7, 
    /* 2268  */ 0xFF5A00A4, 
    /* 2269  */ 0xFF5D00A1, 
    /* 2270  */ 0xFF60009E, 
    /* 2271  */ 0xFF63009B, 
    /* 2272  */ 0xFF670097, 
    /* 2273  */ 0xFF6A0094, 
    /* 2274  */ 0xFF6D0091, 
    /* 2275  */ 0xFF70008E, 
    /* 2276  */ 0xFF73008B, 
    /* 2277  */ 0xFF760088, 
    /* 2278  */ 0xFF790085, 
    /* 2279  */ 0xFF7C0082, 
    /* 2280  */ 0xFF7F007F, 
    /* 2281  */ 0xFF82007C, 
    /* 2282  */ 0xFF850079, 
    /* 2283  */ 0xFF880076, 
    /* 2284  */ 0xFF8B0073, 
    /* 2285  */ 0xFF8E0070, 
    /* 2286  */ 0xFF91006D, 
    /* 2287  */ 0xFF94006A, 
    /* 2288  */ 0xFF970067, 
    /* 2289  */ 0xFF9B0063, 
    /* 2290  */ 0xFF9E0060, 
    /* 2291  */ 0xFFA1005D, 
    /* 2292  */ 0xFFA4005A, 
    /* 2293  */ 0xFFA70057, 
    /* 2294  */ 0xFFAA0054, 
    /* 2295  */ 0xFFAD0051, 
    /* 2296  */ 0xFFB0004E, 
    /* 2297  */ 0xFFB3004B, 
    /* 2298  */ 0xFFB60048, 
    /* 2299  */ 0xFFB90045, 
    /* 2300  */ 0xFFBC0042, 
    /* 2301  */ 0xFFBF003F, 
    /* 2302  */ 0xFFC2003C, 
    /* 2303  */ 0xFFC50039, 
    /* 2304  */ 0xFFC80036, 
    /* 2305  */ 0xFFCB0033, 
    /* 2306  */ 0xFFCF002F, 
    /* 2307  */ 0xFFD2002C, 
    /* 2308  */ 0xFFD50029, 
    /* 2309  */ 0xFFD80026, 
    /* 2310  */ 0xFFDB0023, 
    /* 2311  */ 0xFFDE0020, 
    /* 2312  */ 0xFFE1001D, 
    /* 2313  */ 0xFFE4001A, 
    /* 2314  */ 0xFFE70017, 
    /* 2315  */ 0xFFEA0014, 
    /* 2316  */ 0xFFED0011, 
    /* 2317  */ 0xFFF0000E, 
    /* 2318  */ 0xFFF3000B, 
    /* 2319  */ 0xFFF60008, 
    /* 2320  */ 0xFFF90005, 
    /* 2321  */ 0xFFFC0002, 
    /* 2322  */ 0xFFFD0100, 
    /* 2323  */ 0xFFFA0400, 
    /* 2324  */ 0xFFF70700, 
    /* 2325  */ 0xFFF40A00, 
    /* 2326  */ 0xFFF10D00, 
    /* 2327  */ 0xFFEE1000, 
    /* 2328  */ 0xFFEB1300, 
    /* 2329  */ 0xFFE81600, 
    /* 2330  */ 0xFFE51900, 
    /* 2331  */ 0xFFE21C00, 
    /* 2332  */ 0xFFDF1F00, 
    /* 2333  */ 0xFFDC2200, 
    /* 2334  */ 0xFFD92500, 
    /* 2335  */ 0xFFD62800, 
    /* 2336  */ 0xFFD32B00, 
    /* 2337  */ 0xFFD02E00, 
    /* 2338  */ 0xFFCD3100, 
    /* 2339  */ 0xFFC93500, 
    /* 2340  */ 0xFFC63800, 
    /* 2341  */ 0xFFC33B00, 
    /* 2342  */ 0xFFC03E00, 
    /* 2343  */ 0xFFBD4100, 
    /* 2344  */ 0xFFBA4400, 
    /* 2345  */ 0xFFB74700, 
    /* 2346  */ 0xFFB44A00, 
    /* 2347  */ 0xFFB14D00, 
    /* 2348  */ 0xFFAE5000, 
    /* 2349  */ 0xFFAB5300, 
    /* 2350  */ 0xFFA85600, 
    /* 2351  */ 0xFFA55900, 
    /* 2352  */ 0xFFA25C00, 
    /* 2353  */ 0xFF9F5F00, 
    /* 2354  */ 0xFF9C6200, 
    /* 2355  */ 0xFF996500, 
    /* 2356  */ 0xFF956900, 
    /* 2357  */ 0xFF926C00, 
    /* 2358  */ 0xFF8F6F00, 
    /* 2359  */ 0xFF8C7200, 
    /* 2360  */ 0xFF897500, 
    /* 2361  */ 0xFF867800, 
    /* 2362  */ 0xFF837B00, 
    /* 2363  */ 0xFF807E00, 
    /* 2364  */ 0xFF7D8100, 
    /* 2365  */ 0xFF7A8400, 
    /* 2366  */ 0xFF778700, 
    /* 2367  */ 0xFF748A00, 
    /* 2368  */ 0xFF718D00, 
    /* 2369  */ 0xFF6E9000, 
    /* 2370  */ 0xFF6B9300, 
    /* 2371  */ 0xFF689600, 
    /* 2372  */ 0xFF649A00, 
    /* 2373  */ 0xFF619D00, 
    /* 2374  */ 0xFF5EA000, 
    /* 2375  */ 0xFF5BA300, 
    /* 2376  */ 0xFF58A600, 
    /* 2377  */ 0xFF55A900, 
    /* 2378  */ 0xFF52AC00, 
    /* 2379  */ 0xFF4FAF00, 
    /* 2380  */ 0xFF4CB200, 
    /* 2381  */ 0xFF49B500, 
    /* 2382  */ 0xFF46B800, 
    /* 2383  */ 0xFF43BB00, 
    /* 2384  */ 0xFF40BE00, 
    /* 2385  */ 0xFF3DC100, 
    /* 2386  */ 0xFF3AC400, 
    /* 2387  */ 0xFF37C700, 
    /* 2388  */ 0xFF34CA00, 
    /* 2389  */ 0xFF30CE00, 
    /* 2390  */ 0xFF2DD100, 
    /* 2391  */ 0xFF2AD400, 
    /* 2392  */ 0xFF27D700, 
    /* 2393  */ 0xFF24DA00, 
    /* 2394  */ 0xFF21DD00, 
    /* 2395  */ 0xFF1EE000, 
    /* 2396  */ 0xFF1BE300, 
    /* 2397  */ 0xFF18E600, 
    /* 2398  */ 0xFF15E900, 
    /* 2399  */ 0xFF12EC00, 
    /* 2400  */ 0xFF0FEF00, 
    /* 2401  */ 0xFF0CF200, 
    /* 2402  */ 0xFF09F500, 
    /* 2403  */ 0xFF06F800, 
    /* 2404  */ 0xFF03FB00, 
    /* 2405  */ 0xFF00FE00, 
    /* 2406  */ 0xFF03FB03, 
    /* 2407  */ 0xFF06F806, 
    /* 2408  */ 0xFF09F509, 
    /* 2409  */ 0xFF0CF20C, 
    /* 2410  */ 0xFF0FEF0F, 
    /* 2411  */ 0xFF12EC12, 
    /* 2412  */ 0xFF15E915, 
    /* 2413  */ 0xFF18E618, 
    /* 2414  */ 0xFF1BE31B, 
    /* 2415  */ 0xFF1EE01E, 
    /* 2416  */ 0xFF21DD21, 
    /* 2417  */ 0xFF24DA24, 
    /* 2418  */ 0xFF27D727, 
    /* 2419  */ 0xFF2AD42A, 
    /* 2420  */ 0xFF2DD12D, 
    /* 2421  */ 0xFF30CE30, 
    /* 2422  */ 0xFF34CA34, 
    /* 2423  */ 0xFF37C737, 
    /* 2424  */ 0xFF3AC43A, 
    /* 2425  */ 0xFF3DC13D, 
    /* 2426  */ 0xFF40BE40, 
    /* 2427  */ 0xFF43BB43, 
    /* 2428  */ 0xFF46B846, 
    /* 2429  */ 0xFF49B549, 
    /* 2430  */ 0xFF4CB24C, 
    /* 2431  */ 0xFF4FAF4F, 
    /* 2432  */ 0xFF52AC52, 
    /* 2433  */ 0xFF55A955, 
    /* 2434  */ 0xFF58A658, 
    /* 2435  */ 0xFF5BA35B, 
    /* 2436  */ 0xFF5EA05E, 
    /* 2437  */ 0xFF619D61, 
    /* 2438  */ 0xFF649A64, 
    /* 2439  */ 0xFF689668, 
    /* 2440  */ 0xFF6B936B, 
    /* 2441  */ 0xFF6E906E, 
    /* 2442  */ 0xFF718D71, 
    /* 2443  */ 0xFF748A74, 
    /* 2444  */ 0xFF778777, 
    /* 2445  */ 0xFF7A847A, 
    /* 2446  */ 0xFF7D817D, 
    /* 2447  */ 0xFF807E80, 
    /* 2448  */ 0xFF837B83, 
    /* 2449  */ 0xFF867886, 
    /* 2450  */ 0xFF897589, 
    /* 2451  */ 0xFF8C728C, 
    /* 2452  */ 0xFF8F6F8F, 
    /* 2453  */ 0xFF926C92, 
    /* 2454  */ 0xFF956995, 
    /* 2455  */ 0xFF986698, 
    /* 2456  */ 0xFF9C629C, 
    /* 2457  */ 0xFF9F5F9F, 
    /* 2458  */ 0xFFA25CA2, 
    /* 2459  */ 0xFFA559A5, 
    /* 2460  */ 0xFFA856A8, 
    /* 2461  */ 0xFFAB53AB, 
    /* 2462  */ 0xFFAE50AE, 
    /* 2463  */ 0xFFB14DB1, 
    /* 2464  */ 0xFFB44AB4, 
    /* 2465  */ 0xFFB747B7, 
    /* 2466  */ 0xFFBA44BA, 
    /* 2467  */ 0xFFBD41BD, 
    /* 2468  */ 0xFFC03EC0, 
    /* 2469  */ 0xFFC33BC3, 
    /* 2470  */ 0xFFC638C6, 
    /* 2471  */ 0xFFC935C9, 
    /* 2472  */ 0xFFCD31CD, 
    /* 2473  */ 0xFFD02ED0, 
    /* 2474  */ 0xFFD32BD3, 
    /* 2475  */ 0xFFD628D6, 
    /* 2476  */ 0xFFD925D9, 
    /* 2477  */ 0xFFDC22DC, 
    /* 2478  */ 0xFFDF1FDF, 
    /* 2479  */ 0xFFE21CE2, 
    /* 2480  */ 0xFFE519E5, 
    /* 2481  */ 0xFFE816E8, 
    /* 2482  */ 0xFFEB13EB, 
    /* 2483  */ 0xFFEE10EE, 
    /* 2484  */ 0xFFF10DF1, 
    /* 2485  */ 0xFFF40AF4, 
    /* 2486  */ 0xFFF707F7, 
    /* 2487  */ 0xFFFA04FA, 
    /* 2488  */ 0xFFFD01FD, 
    /* 2489  */ 0xFFFC02FF, 
    /* 2490  */ 0xFFF905FF, 
    /* 2491  */ 0xFFF608FF, 
    /* 2492  */ 0xFFF30BFF, 
    /* 2493  */ 0xFFF00EFF, 
    /* 2494  */ 0xFFED11FF, 
    /* 2495  */ 0xFFEA14FF, 
    /* 2496  */ 0xFFE717FF, 
    /* 2497  */ 0xFFE41AFF, 
    /* 2498  */ 0xFFE11DFF, 
    /* 2499  */ 0xFFDE20FF, 
    /* 2500  */ 0xFFDB23FF, 
    /* 2501  */ 0xFFD826FF, 
    /* 2502  */ 0xFFD529FF, 
    /* 2503  */ 0xFFD22CFF, 
    /* 2504  */ 0xFFCF2FFF, 
    /* 2505  */ 0xFFCC32FF, 
    /* 2506  */ 0xFFC836FF, 
    /* 2507  */ 0xFFC539FF, 
    /* 2508  */ 0xFFC23CFF, 
    /* 2509  */ 0xFFBF3FFF, 
    /* 2510  */ 0xFFBC42FF, 
    /* 2511  */ 0xFFB945FF, 
    /* 2512  */ 0xFFB648FF, 
    /* 2513  */ 0xFFB34BFF, 
    /* 2514  */ 0xFFB04EFF, 
    /* 2515  */ 0xFFAD51FF, 
    /* 2516  */ 0xFFAA54FF, 
    /* 2517  */ 0xFFA757FF, 
    /* 2518  */ 0xFFA45AFF, 
    /* 2519  */ 0xFFA15DFF, 
    /* 2520  */ 0xFF9E60FF, 
    /* 2521  */ 0xFF9B63FF, 
    /* 2522  */ 0xFF9767FF, 
    /* 2523  */ 0xFF946AFF, 
    /* 2524  */ 0xFF916DFF, 
    /* 2525  */ 0xFF8E70FF, 
    /* 2526  */ 0xFF8B73FF, 
    /* 2527  */ 0xFF8876FF, 
    /* 2528  */ 0xFF8579FF, 
    /* 2529  */ 0xFF827CFF, 
    /* 2530  */ 0xFF7F7FFF, 
    /* 2531  */ 0xFF7C82FF, 
    /* 2532  */ 0xFF7985FF, 
    /* 2533  */ 0xFF7688FF, 
    /* 2534  */ 0xFF738BFF, 
    /* 2535  */ 0xFF708EFF, 
    /* 2536  */ 0xFF6D91FF, 
    /* 2537  */ 0xFF6A94FF, 
    /* 2538  */ 0xFF6797FF, 
    /* 2539  */ 0xFF639BFF, 
    /* 2540  */ 0xFF609EFF, 
    /* 2541  */ 0xFF5DA1FF, 
    /* 2542  */ 0xFF5AA4FF, 
    /* 2543  */ 0xFF57A7FF, 
    /* 2544  */ 0xFF54AAFF, 
    /* 2545  */ 0xFF51ADFF, 
    /* 2546  */ 0xFF4EB0FF, 
    /* 2547  */ 0xFF4BB3FF, 
    /* 2548  */ 0xFF48B6FF, 
    /* 2549  */ 0xFF45B9FF, 
    /* 2550  */ 0xFF42BCFF, 
    /* 2551  */ 0xFF3FBFFF, 
    /* 2552  */ 0xFF3CC2FF, 
    /* 2553  */ 0xFF39C5FF, 
    /* 2554  */ 0xFF36C8FF, 
    /* 2555  */ 0xFF33CBFF, 
    /* 2556  */ 0xFF2FCFFF, 
    /* 2557  */ 0xFF2CD2FF, 
    /* 2558  */ 0xFF29D5FF, 
    /* 2559  */ 0xFF26D8FF, 
    /* 2560  */ 0xFF23DBFF, 
    /* 2561  */ 0xFF20DEFF, 
    /* 2562  */ 0xFF1DE1FF, 
    /* 2563  */ 0xFF1AE4FF, 
    /* 2564  */ 0xFF17E7FF, 
    /* 2565  */ 0xFF14EAFF, 
    /* 2566  */ 0xFF11EDFF, 
    /* 2567  */ 0xFF0EF0FF, 
    /* 2568  */ 0xFF0BF3FF, 
    /* 2569  */ 0xFF08F6FF, 
    /* 2570  */ 0xFF05F9FF, 
    /* 2571  */ 0xFF02FCFF, 
    /* 2572  */ 0xFF01FFFD, 
    /* 2573  */ 0xFF04FFFA, 
    /* 2574  */ 0xFF07FFF7, 
    /* 2575  */ 0xFF0AFFF4, 
    /* 2576  */ 0xFF0DFFF1, 
    /* 2577  */ 0xFF10FFEE, 
    /* 2578  */ 0xFF13FFEB, 
    /* 2579  */ 0xFF16FFE8, 
    /* 2580  */ 0xFF19FFE5, 
    /* 2581  */ 0xFF1CFFE2, 
    /* 2582  */ 0xFF1FFFDF, 
    /* 2583  */ 0xFF22FFDC, 
    /* 2584  */ 0xFF25FFD9, 
    /* 2585  */ 0xFF28FFD6, 
    /* 2586  */ 0xFF2BFFD3, 
    /* 2587  */ 0xFF2EFFD0, 
    /* 2588  */ 0xFF31FFCD, 
    /* 2589  */ 0xFF35FFC9, 
    /* 2590  */ 0xFF38FFC6, 
    /* 2591  */ 0xFF3BFFC3, 
    /* 2592  */ 0xFF3EFFC0, 
    /* 2593  */ 0xFF41FFBD, 
    /* 2594  */ 0xFF44FFBA, 
    /* 2595  */ 0xFF47FFB7, 
    /* 2596  */ 0xFF4AFFB4, 
    /* 2597  */ 0xFF4DFFB1, 
    /* 2598  */ 0xFF50FFAE, 
    /* 2599  */ 0xFF53FFAB, 
    /* 2600  */ 0xFF56FFA8, 
    /* 2601  */ 0xFF59FFA5, 
    /* 2602  */ 0xFF5CFFA2, 
    /* 2603  */ 0xFF5FFF9F, 
    /* 2604  */ 0xFF62FF9C, 
    /* 2605  */ 0xFF65FF99, 
    /* 2606  */ 0xFF69FF95, 
    /* 2607  */ 0xFF6CFF92, 
    /* 2608  */ 0xFF6FFF8F, 
    /* 2609  */ 0xFF72FF8C, 
    /* 2610  */ 0xFF75FF89, 
    /* 2611  */ 0xFF78FF86, 
    /* 2612  */ 0xFF7BFF83, 
    /* 2613  */ 0xFF7EFF80, 
    /* 2614  */ 0xFF81FF7D, 
    /* 2615  */ 0xFF84FF7A, 
    /* 2616  */ 0xFF87FF77, 
    /* 2617  */ 0xFF8AFF74, 
    /* 2618  */ 0xFF8DFF71, 
    /* 2619  */ 0xFF90FF6E, 
    /* 2620  */ 0xFF93FF6B, 
    /* 2621  */ 0xFF96FF68, 
    /* 2622  */ 0xFF9AFF64, 
    /* 2623  */ 0xFF9DFF61, 
    /* 2624  */ 0xFFA0FF5E, 
    /* 2625  */ 0xFFA3FF5B, 
    /* 2626  */ 0xFFA6FF58, 
    /* 2627  */ 0xFFA9FF55, 
    /* 2628  */ 0xFFACFF52, 
    /* 2629  */ 0xFFAFFF4F, 
    /* 2630  */ 0xFFB2FF4C, 
    /* 2631  */ 0xFFB5FF49, 
    /* 2632  */ 0xFFB8FF46, 
    /* 2633  */ 0xFFBBFF43, 
    /* 2634  */ 0xFFBEFF40, 
    /* 2635  */ 0xFFC1FF3D, 
    /* 2636  */ 0xFFC4FF3A, 
    /* 2637  */ 0xFFC7FF37, 
    /* 2638  */ 0xFFCAFF34, 
    /* 2639  */ 0xFFCEFF30, 
    /* 2640  */ 0xFFD1FF2D, 
    /* 2641  */ 0xFFD4FF2A, 
    /* 2642  */ 0xFFD7FF27, 
    /* 2643  */ 0xFFDAFF24, 
    /* 2644  */ 0xFFDDFF21, 
    /* 2645  */ 0xFFE0FF1E, 
    /* 2646  */ 0xFFE3FF1B, 
    /* 2647  */ 0xFFE6FF18, 
    /* 2648  */ 0xFFE9FF15, 
    /* 2649  */ 0xFFECFF12, 
    /* 2650  */ 0xFFEFFF0F, 
    /* 2651  */ 0xFFF2FF0C, 
    /* 2652  */ 0xFFF5FF09, 
    /* 2653  */ 0xFFF8FF06, 
    /* 2654  */ 0xFFFBFF03, 
    /* 2655  */ 0xFFFEFF00, 
    /* 2656  */ 0xFFFBFF00, 
    /* 2657  */ 0xFFF8FF00, 
    /* 2658  */ 0xFFF5FF00, 
    /* 2659  */ 0xFFF2FF00, 
    /* 2660  */ 0xFFEFFF00, 
    /* 2661  */ 0xFFECFF00, 
    /* 2662  */ 0xFFE9FF00, 
    /* 2663  */ 0xFFE6FF00, 
    /* 2664  */ 0xFFE3FF00, 
    /* 2665  */ 0xFFE0FF00, 
    /* 2666  */ 0xFFDDFF00, 
    /* 2667  */ 0xFFDAFF00, 
    /* 2668  */ 0xFFD7FF00, 
    /* 2669  */ 0xFFD4FF00, 
    /* 2670  */ 0xFFD1FF00, 
    /* 2671  */ 0xFFCEFF00, 
    /* 2672  */ 0xFFCAFF00, 
    /* 2673  */ 0xFFC7FF00, 
    /* 2674  */ 0xFFC4FF00, 
    /* 2675  */ 0xFFC1FF00, 
    /* 2676  */ 0xFFBEFF00, 
    /* 2677  */ 0xFFBBFF00, 
    /* 2678  */ 0xFFB8FF00, 
    /* 2679  */ 0xFFB5FF00, 
    /* 2680  */ 0xFFB2FF00, 
    /* 2681  */ 0xFFAFFF00, 
    /* 2682  */ 0xFFACFF00, 
    /* 2683  */ 0xFFA9FF00, 
    /* 2684  */ 0xFFA6FF00, 
    /* 2685  */ 0xFFA3FF00, 
    /* 2686  */ 0xFFA0FF00, 
    /* 2687  */ 0xFF9DFF00, 
    /* 2688  */ 0xFF9AFF00, 
    /* 2689  */ 0xFF96FF00, 
    /* 2690  */ 0xFF93FF00, 
    /* 2691  */ 0xFF90FF00, 
    /* 2692  */ 0xFF8DFF00, 
    /* 2693  */ 0xFF8AFF00, 
    /* 2694  */ 0xFF87FF00, 
    /* 2695  */ 0xFF84FF00, 
    /* 2696  */ 0xFF81FF00, 
    /* 2697  */ 0xFF7EFF00, 
    /* 2698  */ 0xFF7BFF00, 
    /* 2699  */ 0xFF78FF00, 
    /* 2700  */ 0xFF75FF00, 
    /* 2701  */ 0xFF72FF00, 
    /* 2702  */ 0xFF6FFF00, 
    /* 2703  */ 0xFF6CFF00, 
    /* 2704  */ 0xFF69FF00, 
    /* 2705  */ 0xFF66FF00, 
    /* 2706  */ 0xFF62FF00, 
    /* 2707  */ 0xFF5FFF00, 
    /* 2708  */ 0xFF5CFF00, 
    /* 2709  */ 0xFF59FF00, 
    /* 2710  */ 0xFF56FF00, 
    /* 2711  */ 0xFF53FF00, 
    /* 2712  */ 0xFF50FF00, 
    /* 2713  */ 0xFF4DFF00, 
    /* 2714  */ 0xFF4AFF00, 
    /* 2715  */ 0xFF47FF00, 
    /* 2716  */ 0xFF44FF00, 
    /* 2717  */ 0xFF41FF00, 
    /* 2718  */ 0xFF3EFF00, 
    /* 2719  */ 0xFF3BFF00, 
    /* 2720  */ 0xFF38FF00, 
    /* 2721  */ 0xFF35FF00, 
    /* 2722  */ 0xFF31FF00, 
    /* 2723  */ 0xFF2EFF00, 
    /* 2724  */ 0xFF2BFF00, 
    /* 2725  */ 0xFF28FF00, 
    /* 2726  */ 0xFF25FF00, 
    /* 2727  */ 0xFF22FF00, 
    /* 2728  */ 0xFF1FFF00, 
    /* 2729  */ 0xFF1CFF00, 
    /* 2730  */ 0xFF19FF00, 
    /* 2731  */ 0xFF16FF00, 
    /* 2732  */ 0xFF13FF00, 
    /* 2733  */ 0xFF10FF00, 
    /* 2734  */ 0xFF0DFF00, 
    /* 2735  */ 0xFF0AFF00, 
    /* 2736  */ 0xFF07FF00, 
    /* 2737  */ 0xFF04FF00, 
    /* 2738  */ 0xFF01FF00, 
    /* 2739  */ 0xFF00FC02, 
    /* 2740  */ 0xFF00F905, 
    /* 2741  */ 0xFF00F608, 
    /* 2742  */ 0xFF00F30B, 
    /* 2743  */ 0xFF00F00E, 
    /* 2744  */ 0xFF00ED11, 
    /* 2745  */ 0xFF00EA14, 
    /* 2746  */ 0xFF00E717, 
    /* 2747  */ 0xFF00E41A, 
    /* 2748  */ 0xFF00E11D, 
    /* 2749  */ 0xFF00DE20, 
    /* 2750  */ 0xFF00DB23, 
    /* 2751  */ 0xFF00D826, 
    /* 2752  */ 0xFF00D529, 
    /* 2753  */ 0xFF00D22C, 
    /* 2754  */ 0xFF00CF2F, 
    /* 2755  */ 0xFF00CC32, 
    /* 2756  */ 0xFF00C836, 
    /* 2757  */ 0xFF00C539, 
    /* 2758  */ 0xFF00C23C, 
    /* 2759  */ 0xFF00BF3F, 
    /* 2760  */ 0xFF00BC42, 
    /* 2761  */ 0xFF00B945, 
    /* 2762  */ 0xFF00B648, 
    /* 2763  */ 0xFF00B34B, 
    /* 2764  */ 0xFF00B04E, 
    /* 2765  */ 0xFF00AD51, 
    /* 2766  */ 0xFF00AA54, 
    /* 2767  */ 0xFF00A757, 
    /* 2768  */ 0xFF00A45A, 
    /* 2769  */ 0xFF00A15D, 
    /* 2770  */ 0xFF009E60, 
    /* 2771  */ 0xFF009B63, 
    /* 2772  */ 0xFF009767, 
    /* 2773  */ 0xFF00946A, 
    /* 2774  */ 0xFF00916D, 
    /* 2775  */ 0xFF008E70, 
    /* 2776  */ 0xFF008B73, 
    /* 2777  */ 0xFF008876, 
    /* 2778  */ 0xFF008579, 
    /* 2779  */ 0xFF00827C, 
    /* 2780  */ 0xFF007F7F, 
    /* 2781  */ 0xFF007C82, 
    /* 2782  */ 0xFF007985, 
    /* 2783  */ 0xFF007688, 
    /* 2784  */ 0xFF00738B, 
    /* 2785  */ 0xFF00708E, 
    /* 2786  */ 0xFF006D91, 
    /* 2787  */ 0xFF006A94, 
    /* 2788  */ 0xFF006797, 
    /* 2789  */ 0xFF00639B, 
    /* 2790  */ 0xFF00609E, 
    /* 2791  */ 0xFF005DA1, 
    /* 2792  */ 0xFF005AA4, 
    /* 2793  */ 0xFF0057A7, 
    /* 2794  */ 0xFF0054AA, 
    /* 2795  */ 0xFF0051AD, 
    /* 2796  */ 0xFF004EB0, 
    /* 2797  */ 0xFF004BB3, 
    /* 2798  */ 0xFF0048B6, 
    /* 2799  */ 0xFF0045B9, 
    /* 2800  */ 0xFF0042BC, 
    /* 2801  */ 0xFF003FBF, 
    /* 2802  */ 0xFF003CC2, 
    /* 2803  */ 0xFF0039C5, 
    /* 2804  */ 0xFF0036C8, 
    /* 2805  */ 0xFF0033CB, 
    /* 2806  */ 0xFF002FCF, 
    /* 2807  */ 0xFF002CD2, 
    /* 2808  */ 0xFF0029D5, 
    /* 2809  */ 0xFF0026D8, 
    /* 2810  */ 0xFF0023DB, 
    /* 2811  */ 0xFF0020DE, 
    /* 2812  */ 0xFF001DE1, 
    /* 2813  */ 0xFF001AE4, 
    /* 2814  */ 0xFF0017E7, 
    /* 2815  */ 0xFF0014EA, 
    /* 2816  */ 0xFF0011ED, 
    /* 2817  */ 0xFF000EF0, 
    /* 2818  */ 0xFF000BF3, 
    /* 2819  */ 0xFF0008F6, 
    /* 2820  */ 0xFF0005F9, 
    /* 2821  */ 0xFF0002FC, 
    /* 2822  */ 0xFF0100FF, 
    /* 2823  */ 0xFF0400FF, 
    /* 2824  */ 0xFF0700FF, 
    /* 2825  */ 0xFF0A00FF, 
    /* 2826  */ 0xFF0D00FF, 
    /* 2827  */ 0xFF1000FF, 
    /* 2828  */ 0xFF1300FF, 
    /* 2829  */ 0xFF1600FF, 
    /* 2830  */ 0xFF1900FF, 
    /* 2831  */ 0xFF1C00FF, 
    /* 2832  */ 0xFF1F00FF, 
    /* 2833  */ 0xFF2200FF, 
    /* 2834  */ 0xFF2500FF, 
    /* 2835  */ 0xFF2800FF, 
    /* 2836  */ 0xFF2B00FF, 
    /* 2837  */ 0xFF2E00FF, 
    /* 2838  */ 0xFF3100FF, 
    /* 2839  */ 0xFF3500FF, 
    /* 2840  */ 0xFF3800FF, 
    /* 2841  */ 0xFF3B00FF, 
    /* 2842  */ 0xFF3E00FF, 
    /* 2843  */ 0xFF4100FF, 
    /* 2844  */ 0xFF4400FF, 
    /* 2845  */ 0xFF4700FF, 
    /* 2846  */ 0xFF4A00FF, 
    /* 2847  */ 0xFF4D00FF, 
    /* 2848  */ 0xFF5000FF, 
    /* 2849  */ 0xFF5300FF, 
    /* 2850  */ 0xFF5600FF, 
    /* 2851  */ 0xFF5900FF, 
    /* 2852  */ 0xFF5C00FF, 
    /* 2853  */ 0xFF5F00FF, 
    /* 2854  */ 0xFF6200FF, 
    /* 2855  */ 0xFF6500FF, 
    /* 2856  */ 0xFF6900FF, 
    /* 2857  */ 0xFF6C00FF, 
    /* 2858  */ 0xFF6F00FF, 
    /* 2859  */ 0xFF7200FF, 
    /* 2860  */ 0xFF7500FF, 
    /* 2861  */ 0xFF7800FF, 
    /* 2862  */ 0xFF7B00FF, 
    /* 2863  */ 0xFF7E00FF, 
    /* 2864  */ 0xFF8100FF, 
    /* 2865  */ 0xFF8400FF, 
    /* 2866  */ 0xFF8700FF, 
    /* 2867  */ 0xFF8A00FF, 
    /* 2868  */ 0xFF8D00FF, 
    /* 2869  */ 0xFF9000FF, 
    /* 2870  */ 0xFF9300FF, 
    /* 2871  */ 0xFF9600FF, 
    /* 2872  */ 0xFF9A00FF, 
    /* 2873  */ 0xFF9D00FF, 
    /* 2874  */ 0xFFA000FF, 
    /* 2875  */ 0xFFA300FF, 
    /* 2876  */ 0xFFA600FF, 
    /* 2877  */ 0xFFA900FF, 
    /* 2878  */ 0xFFAC00FF, 
    /* 2879  */ 0xFFAF00FF, 
    /* 2880  */ 0xFFB200FF, 
    /* 2881  */ 0xFFB500FF, 
    /* 2882  */ 0xFFB800FF, 
    /* 2883  */ 0xFFBB00FF, 
    /* 2884  */ 0xFFBE00FF, 
    /* 2885  */ 0xFFC100FF, 
    /* 2886  */ 0xFFC400FF, 
    /* 2887  */ 0xFFC700FF, 
    /* 2888  */ 0xFFCA00FF, 
    /* 2889  */ 0xFFCE00FF, 
    /* 2890  */ 0xFFD100FF, 
    /* 2891  */ 0xFFD400FF, 
    /* 2892  */ 0xFFD700FF, 
    /* 2893  */ 0xFFDA00FF, 
    /* 2894  */ 0xFFDD00FF, 
    /* 2895  */ 0xFFE000FF, 
    /* 2896  */ 0xFFE300FF, 
    /* 2897  */ 0xFFE600FF, 
    /* 2898  */ 0xFFE900FF, 
    /* 2899  */ 0xFFEC00FF, 
    /* 2900  */ 0xFFEF00FF, 
    /* 2901  */ 0xFFF200FF, 
    /* 2902  */ 0xFFF500FF, 
    /* 2903  */ 0xFFF800FF, 
    /* 2904  */ 0xFFFB00FF, 
    /* 2905  */ 0xFFFE00FF, 
    /* 2906  */ 0xFFFF03FB, 
    /* 2907  */ 0xFFFF06F8, 
    /* 2908  */ 0xFFFF09F5, 
    /* 2909  */ 0xFFFF0CF2, 
    /* 2910  */ 0xFFFF0FEF, 
    /* 2911  */ 0xFFFF12EC, 
    /* 2912  */ 0xFFFF15E9, 
    /* 2913  */ 0xFFFF18E6, 
    /* 2914  */ 0xFFFF1BE3, 
    /* 2915  */ 0xFFFF1EE0, 
    /* 2916  */ 0xFFFF21DD, 
    /* 2917  */ 0xFFFF24DA, 
    /* 2918  */ 0xFFFF27D7, 
    /* 2919  */ 0xFFFF2AD4, 
    /* 2920  */ 0xFFFF2DD1, 
    /* 2921  */ 0xFFFF30CE, 
    /* 2922  */ 0xFFFF34CA, 
    /* 2923  */ 0xFFFF37C7, 
    /* 2924  */ 0xFFFF3AC4, 
    /* 2925  */ 0xFFFF3DC1, 
    /* 2926  */ 0xFFFF40BE, 
    /* 2927  */ 0xFFFF43BB, 
    /* 2928  */ 0xFFFF46B8, 
    /* 2929  */ 0xFFFF49B5, 
    /* 2930  */ 0xFFFF4CB2, 
    /* 2931  */ 0xFFFF4FAF, 
    /* 2932  */ 0xFFFF52AC, 
    /* 2933  */ 0xFFFF55A9, 
    /* 2934  */ 0xFFFF58A6, 
    /* 2935  */ 0xFFFF5BA3, 
    /* 2936  */ 0xFFFF5EA0, 
    /* 2937  */ 0xFFFF619D, 
    /* 2938  */ 0xFFFF649A, 
    /* 2939  */ 0xFFFF6896, 
    /* 2940  */ 0xFFFF6B93, 
    /* 2941  */ 0xFFFF6E90, 
    /* 2942  */ 0xFFFF718D, 
    /* 2943  */ 0xFFFF748A, 
    /* 2944  */ 0xFFFF7787, 
    /* 2945  */ 0xFFFF7A84, 
    /* 2946  */ 0xFFFF7D81, 
    /* 2947  */ 0xFFFF807E, 
    /* 2948  */ 0xFFFF837B, 
    /* 2949  */ 0xFFFF8678, 
    /* 2950  */ 0xFFFF8975, 
    /* 2951  */ 0xFFFF8C72, 
    /* 2952  */ 0xFFFF8F6F, 
    /* 2953  */ 0xFFFF926C, 
    /* 2954  */ 0xFFFF9569, 
    /* 2955  */ 0xFFFF9866, 
    /* 2956  */ 0xFFFF9C62, 
    /* 2957  */ 0xFFFF9F5F, 
    /* 2958  */ 0xFFFFA25C, 
    /* 2959  */ 0xFFFFA559, 
    /* 2960  */ 0xFFFFA856, 
    /* 2961  */ 0xFFFFAB53, 
    /* 2962  */ 0xFFFFAE50, 
    /* 2963  */ 0xFFFFB14D, 
    /* 2964  */ 0xFFFFB44A, 
    /* 2965  */ 0xFFFFB747, 
    /* 2966  */ 0xFFFFBA44, 
    /* 2967  */ 0xFFFFBD41, 
    /* 2968  */ 0xFFFFC03E, 
    /* 2969  */ 0xFFFFC33B, 
    /* 2970  */ 0xFFFFC638, 
    /* 2971  */ 0xFFFFC935, 
    /* 2972  */ 0xFFFFCD31, 
    /* 2973  */ 0xFFFFD02E, 
    /* 2974  */ 0xFFFFD32B, 
    /* 2975  */ 0xFFFFD628, 
    /* 2976  */ 0xFFFFD925, 
    /* 2977  */ 0xFFFFDC22, 
    /* 2978  */ 0xFFFFDF1F, 
    /* 2979  */ 0xFFFFE21C, 
    /* 2980  */ 0xFFFFE519, 
    /* 2981  */ 0xFFFFE816, 
    /* 2982  */ 0xFFFFEB13, 
    /* 2983  */ 0xFFFFEE10, 
    /* 2984  */ 0xFFFFF10D, 
    /* 2985  */ 0xFFFFF40A, 
    /* 2986  */ 0xFFFFF707, 
    /* 2987  */ 0xFFFFFA04, 
    /* 2988  */ 0xFFFFFD01, 
    /* 2989  */ 0xFFFFFC00, 
    /* 2990  */ 0xFFFFF900, 
    /* 2991  */ 0xFFFFF600, 
    /* 2992  */ 0xFFFFF300, 
    /* 2993  */ 0xFFFFF000, 
    /* 2994  */ 0xFFFFED00, 
    /* 2995  */ 0xFFFFEA00, 
    /* 2996  */ 0xFFFFE700, 
    /* 2997  */ 0xFFFFE400, 
    /* 2998  */ 0xFFFFE100, 
    /* 2999  */ 0xFFFFDE00, 
    /* 3000  */ 0xFFFFDB00, 
    /* 3001  */ 0xFFFFD800, 
    /* 3002  */ 0xFFFFD500, 
    /* 3003  */ 0xFFFFD200, 
    /* 3004  */ 0xFFFFCF00, 
    /* 3005  */ 0xFFFFCC00, 
    /* 3006  */ 0xFFFFC800, 
    /* 3007  */ 0xFFFFC500, 
    /* 3008  */ 0xFFFFC200, 
    /* 3009  */ 0xFFFFBF00, 
    /* 3010  */ 0xFFFFBC00, 
    /* 3011  */ 0xFFFFB900, 
    /* 3012  */ 0xFFFFB600, 
    /* 3013  */ 0xFFFFB300, 
    /* 3014  */ 0xFFFFB000, 
    /* 3015  */ 0xFFFFAD00, 
    /* 3016  */ 0xFFFFAA00, 
    /* 3017  */ 0xFFFFA700, 
    /* 3018  */ 0xFFFFA400, 
    /* 3019  */ 0xFFFFA100, 
    /* 3020  */ 0xFFFF9E00, 
    /* 3021  */ 0xFFFF9B00, 
    /* 3022  */ 0xFFFF9700, 
    /* 3023  */ 0xFFFF9400, 
    /* 3024  */ 0xFFFF9100, 
    /* 3025  */ 0xFFFF8E00, 
    /* 3026  */ 0xFFFF8B00, 
    /* 3027  */ 0xFFFF8800, 
    /* 3028  */ 0xFFFF8500, 
    /* 3029  */ 0xFFFF8200, 
    /* 3030  */ 0xFFFF7F00, 
    /* 3031  */ 0xFFFF7C00, 
    /* 3032  */ 0xFFFF7900, 
    /* 3033  */ 0xFFFF7600, 
    /* 3034  */ 0xFFFF7300, 
    /* 3035  */ 0xFFFF7000, 
    /* 3036  */ 0xFFFF6D00, 
    /* 3037  */ 0xFFFF6A00, 
    /* 3038  */ 0xFFFF6700, 
    /* 3039  */ 0xFFFF6300, 
    /* 3040  */ 0xFFFF6000, 
    /* 3041  */ 0xFFFF5D00, 
    /* 3042  */ 0xFFFF5A00, 
    /* 3043  */ 0xFFFF5700, 
    /* 3044  */ 0xFFFF5400, 
    /* 3045  */ 0xFFFF5100, 
    /* 3046  */ 0xFFFF4E00, 
    /* 3047  */ 0xFFFF4B00, 
    /* 3048  */ 0xFFFF4800, 
    /* 3049  */ 0xFFFF4500, 
    /* 3050  */ 0xFFFF4200, 
    /* 3051  */ 0xFFFF3F00, 
    /* 3052  */ 0xFFFF3C00, 
    /* 3053  */ 0xFFFF3900, 
    /* 3054  */ 0xFFFF3600, 
    /* 3055  */ 0xFFFF3300, 
    /* 3056  */ 0xFFFF2F00, 
    /* 3057  */ 0xFFFF2C00, 
    /* 3058  */ 0xFFFF2900, 
    /* 3059  */ 0xFFFF2600, 
    /* 3060  */ 0xFFFF2300, 
    /* 3061  */ 0xFFFF2000, 
    /* 3062  */ 0xFFFF1D00, 
    /* 3063  */ 0xFFFF1A00, 
    /* 3064  */ 0xFFFF1700, 
    /* 3065  */ 0xFFFF1400, 
    /* 3066  */ 0xFFFF1100, 
    /* 3067  */ 0xFFFF0E00, 
    /* 3068  */ 0xFFFF0B00, 
    /* 3069  */ 0xFFFF0800, 
    /* 3070  */ 0xFFFF0500, 
    /* 3071  */ 0xFFFF0200, 
    /* 3072  */ 0xFFFD0101, 
    /* 3073  */ 0xFFFA0404, 
    /* 3074  */ 0xFFF70707, 
    /* 3075  */ 0xFFF40A0A, 
    /* 3076  */ 0xFFF10D0D, 
    /* 3077  */ 0xFFEE1010, 
    /* 3078  */ 0xFFEB1313, 
    /* 3079  */ 0xFFE81616, 
    /* 3080  */ 0xFFE51919, 
    /* 3081  */ 0xFFE21C1C, 
    /* 3082  */ 0xFFDF1F1F, 
    /* 3083  */ 0xFFDC2222, 
    /* 3084  */ 0xFFD92525, 
    /* 3085  */ 0xFFD62828, 
    /* 3086  */ 0xFFD32B2B, 
    /* 3087  */ 0xFFD02E2E, 
    /* 3088  */ 0xFFCD3131, 
    /* 3089  */ 0xFFC93535, 
    /* 3090  */ 0xFFC63838, 
    /* 3091  */ 0xFFC33B3B, 
    /* 3092  */ 0xFFC03E3E, 
    /* 3093  */ 0xFFBD4141, 
    /* 3094  */ 0xFFBA4444, 
    /* 3095  */ 0xFFB74747, 
    /* 3096  */ 0xFFB44A4A, 
    /* 3097  */ 0xFFB14D4D, 
    /* 3098  */ 0xFFAE5050, 
    /* 3099  */ 0xFFAB5353, 
    /* 3100  */ 0xFFA85656, 
    /* 3101  */ 0xFFA55959, 
    /* 3102  */ 0xFFA25C5C, 
    /* 3103  */ 0xFF9F5F5F, 
    /* 3104  */ 0xFF9C6262, 
    /* 3105  */ 0xFF996565, 
    /* 3106  */ 0xFF956969, 
    /* 3107  */ 0xFF926C6C, 
    /* 3108  */ 0xFF8F6F6F, 
    /* 3109  */ 0xFF8C7272, 
    /* 3110  */ 0xFF897575, 
    /* 3111  */ 0xFF867878, 
    /* 3112  */ 0xFF837B7B, 
    /* 3113  */ 0xFF807E7E, 
    /* 3114  */ 0xFF7D8181, 
    /* 3115  */ 0xFF7A8484, 
    /* 3116  */ 0xFF778787, 
    /* 3117  */ 0xFF748A8A, 
    /* 3118  */ 0xFF718D8D, 
    /* 3119  */ 0xFF6E9090, 
    /* 3120  */ 0xFF6B9393, 
    /* 3121  */ 0xFF689696, 
    /* 3122  */ 0xFF649A9A, 
    /* 3123  */ 0xFF619D9D, 
    /* 3124  */ 0xFF5EA0A0, 
    /* 3125  */ 0xFF5BA3A3, 
    /* 3126  */ 0xFF58A6A6, 
    /* 3127  */ 0xFF55A9A9, 
    /* 3128  */ 0xFF52ACAC, 
    /* 3129  */ 0xFF4FAFAF, 
    /* 3130  */ 0xFF4CB2B2, 
    /* 3131  */ 0xFF49B5B5, 
    /* 3132  */ 0xFF46B8B8, 
    /* 3133  */ 0xFF43BBBB, 
    /* 3134  */ 0xFF40BEBE, 
    /* 3135  */ 0xFF3DC1C1, 
    /* 3136  */ 0xFF3AC4C4, 
    /* 3137  */ 0xFF37C7C7, 
    /* 3138  */ 0xFF34CACA, 
    /* 3139  */ 0xFF30CECE, 
    /* 3140  */ 0xFF2DD1D1, 
    /* 3141  */ 0xFF2AD4D4, 
    /* 3142  */ 0xFF27D7D7, 
    /* 3143  */ 0xFF24DADA, 
    /* 3144  */ 0xFF21DDDD, 
    /* 3145  */ 0xFF1EE0E0, 
    /* 3146  */ 0xFF1BE3E3, 
    /* 3147  */ 0xFF18E6E6, 
    /* 3148  */ 0xFF15E9E9, 
    /* 3149  */ 0xFF12ECEC, 
    /* 3150  */ 0xFF0FEFEF, 
    /* 3151  */ 0xFF0CF2F2, 
    /* 3152  */ 0xFF09F5F5, 
    /* 3153  */ 0xFF06F8F8, 
    /* 3154  */ 0xFF03FBFB, 
    /* 3155  */ 0xFFFFFF00, 
    /* 3156  */ 0xFFFFFF06, 
    /* 3157  */ 0xFFFFFF0C, 
    /* 3158  */ 0xFFFFFF12, 
    /* 3159  */ 0xFFFFFF18, 
    /* 3160  */ 0xFFFFFF1E, 
    /* 3161  */ 0xFFFFFF24, 
    /* 3162  */ 0xFFFFFF2A, 
    /* 3163  */ 0xFFFFFF30, 
    /* 3164  */ 0xFFFFFF37, 
    /* 3165  */ 0xFFFFFF3D, 
    /* 3166  */ 0xFFFFFF43, 
    /* 3167  */ 0xFFFFFF49, 
    /* 3168  */ 0xFFFFFF4F, 
    /* 3169  */ 0xFFFFFF55, 
    /* 3170  */ 0xFFFFFF5B, 
    /* 3171  */ 0xFFFFFF61, 
    /* 3172  */ 0xFFFFFF68, 
    /* 3173  */ 0xFFFFFF6E, 
    /* 3174  */ 0xFFFFFF74, 
    /* 3175  */ 0xFFFFFF7A, 
    /* 3176  */ 0xFFFFFF80, 
    /* 3177  */ 0xFFFFFF86, 
    /* 3178  */ 0xFFFFFF8C, 
    /* 3179  */ 0xFFFFFF92, 
    /* 3180  */ 0xFFFFFF98, 
    /* 3181  */ 0xFFFFFF9F, 
    /* 3182  */ 0xFFFFFFA5, 
    /* 3183  */ 0xFFFFFFAB, 
    /* 3184  */ 0xFFFFFFB1, 
    /* 3185  */ 0xFFFFFFB7, 
    /* 3186  */ 0xFFFFFFBD, 
    /* 3187  */ 0xFFFFFFC3, 
    /* 3188  */ 0xFFFFFFC9, 
    /* 3189  */ 0xFFFFFFD0, 
    /* 3190  */ 0xFFFFFFD6, 
    /* 3191  */ 0xFFFFFFDC, 
    /* 3192  */ 0xFFFFFFE2, 
    /* 3193  */ 0xFFFFFFE8, 
    /* 3194  */ 0xFFFFFFEE, 
    /* 3195  */ 0xFFFFFFF4, 
    /* 3196  */ 0xFFFFFFFA, 
    /* 3197  */ 0xFFFCFCFF, 
    /* 3198  */ 0xFFF6F6FF, 
    /* 3199  */ 0xFFF0F0FF, 
    /* 3200  */ 0xFFEAEAFF, 
    /* 3201  */ 0xFFE4E4FF, 
    /* 3202  */ 0xFFDEDEFF, 
    /* 3203  */ 0xFFD8D8FF, 
    /* 3204  */ 0xFFD2D2FF, 
    /* 3205  */ 0xFFCCCCFF, 
    /* 3206  */ 0xFFC5C5FF, 
    /* 3207  */ 0xFFBFBFFF, 
    /* 3208  */ 0xFFB9B9FF, 
    /* 3209  */ 0xFFB3B3FF, 
    /* 3210  */ 0xFFADADFF, 
    /* 3211  */ 0xFFA7A7FF, 
    /* 3212  */ 0xFFA1A1FF, 
    /* 3213  */ 0xFF9B9BFF, 
    /* 3214  */ 0xFF9494FF, 
    /* 3215  */ 0xFF8E8EFF, 
    /* 3216  */ 0xFF8888FF, 
    /* 3217  */ 0xFF8282FF, 
    /* 3218  */ 0xFF7C7CFF, 
    /* 3219  */ 0xFF7676FF, 
    /* 3220  */ 0xFF7070FF, 
    /* 3221  */ 0xFF6A6AFF, 
    /* 3222  */ 0xFF6363FF, 
    /* 3223  */ 0xFF5D5DFF, 
    /* 3224  */ 0xFF5757FF, 
    /* 3225  */ 0xFF5151FF, 
    /* 3226  */ 0xFF4B4BFF, 
    /* 3227  */ 0xFF4545FF, 
    /* 3228  */ 0xFF3F3FFF, 
    /* 3229  */ 0xFF3939FF, 
    /* 3230  */ 0xFF3333FF, 
    /* 3231  */ 0xFF2C2CFF, 
    /* 3232  */ 0xFF2626FF, 
    /* 3233  */ 0xFF2020FF, 
    /* 3234  */ 0xFF1A1AFF, 
    /* 3235  */ 0xFF1414FF, 
    /* 3236  */ 0xFF0E0EFF, 
    /* 3237  */ 0xFF0808FF, 
    /* 3238  */ 0xFF0202FF, 
    /* 3239  */ 0xFF0404FF, 
    /* 3240  */ 0xFF0A0AFF, 
    /* 3241  */ 0xFF1010FF, 
    /* 3242  */ 0xFF1616FF, 
    /* 3243  */ 0xFF1C1CFF, 
    /* 3244  */ 0xFF2222FF, 
    /* 3245  */ 0xFF2828FF, 
    /* 3246  */ 0xFF2E2EFF, 
    /* 3247  */ 0xFF3535FF, 
    /* 3248  */ 0xFF3B3BFF, 
    /* 3249  */ 0xFF4141FF, 
    /* 3250  */ 0xFF4747FF, 
    /* 3251  */ 0xFF4D4DFF, 
    /* 3252  */ 0xFF5353FF, 
    /* 3253  */ 0xFF5959FF, 
    /* 3254  */ 0xFF5F5FFF, 
    /* 3255  */ 0xFF6565FF, 
    /* 3256  */ 0xFF6C6CFF, 
    /* 3257  */ 0xFF7272FF, 
    /* 3258  */ 0xFF7878FF, 
    /* 3259  */ 0xFF7E7EFF, 
    /* 3260  */ 0xFF8484FF, 
    /* 3261  */ 0xFF8A8AFF, 
    /* 3262  */ 0xFF9090FF, 
    /* 3263  */ 0xFF9696FF, 
    /* 3264  */ 0xFF9D9DFF, 
    /* 3265  */ 0xFFA3A3FF, 
    /* 3266  */ 0xFFA9A9FF, 
    /* 3267  */ 0xFFAFAFFF, 
    /* 3268  */ 0xFFB5B5FF, 
    /* 3269  */ 0xFFBBBBFF, 
    /* 3270  */ 0xFFC1C1FF, 
    /* 3271  */ 0xFFC7C7FF, 
    /* 3272  */ 0xFFCECEFF, 
    /* 3273  */ 0xFFD4D4FF, 
    /* 3274  */ 0xFFDADAFF, 
    /* 3275  */ 0xFFE0E0FF, 
    /* 3276  */ 0xFFE6E6FF, 
    /* 3277  */ 0xFFECECFF, 
    /* 3278  */ 0xFFF2F2FF, 
    /* 3279  */ 0xFFF8F8FF, 
    /* 3280  */ 0xFFFEFEFF, 
    /* 3281  */ 0xFFFFF8F8, 
    /* 3282  */ 0xFFFFF2F2, 
    /* 3283  */ 0xFFFFECEC, 
    /* 3284  */ 0xFFFFE6E6, 
    /* 3285  */ 0xFFFFE0E0, 
    /* 3286  */ 0xFFFFDADA, 
    /* 3287  */ 0xFFFFD4D4, 
    /* 3288  */ 0xFFFFCECE, 
    /* 3289  */ 0xFFFFC7C7, 
    /* 3290  */ 0xFFFFC1C1, 
    /* 3291  */ 0xFFFFBBBB, 
    /* 3292  */ 0xFFFFB5B5, 
    /* 3293  */ 0xFFFFAFAF, 
    /* 3294  */ 0xFFFFA9A9, 
    /* 3295  */ 0xFFFFA3A3, 
    /* 3296  */ 0xFFFF9D9D, 
    /* 3297  */ 0xFFFF9696, 
    /* 3298  */ 0xFFFF9090, 
    /* 3299  */ 0xFFFF8A8A, 
    /* 3300  */ 0xFFFF8484, 
    /* 3301  */ 0xFFFF7E7E, 
    /* 3302  */ 0xFFFF7878, 
    /* 3303  */ 0xFFFF7272, 
    /* 3304  */ 0xFFFF6C6C, 
    /* 3305  */ 0xFFFF6666, 
    /* 3306  */ 0xFFFF5F5F, 
    /* 3307  */ 0xFFFF5959, 
    /* 3308  */ 0xFFFF5353, 
    /* 3309  */ 0xFFFF4D4D, 
    /* 3310  */ 0xFFFF4747, 
    /* 3311  */ 0xFFFF4141, 
    /* 3312  */ 0xFFFF3B3B, 
    /* 3313  */ 0xFFFF3535, 
    /* 3314  */ 0xFFFF2E2E, 
    /* 3315  */ 0xFFFF2828, 
    /* 3316  */ 0xFFFF2222, 
    /* 3317  */ 0xFFFF1C1C, 
    /* 3318  */ 0xFFFF1616, 
    /* 3319  */ 0xFFFF1010, 
    /* 3320  */ 0xFFFF0A0A, 
    /* 3321  */ 0xFFFF0404, 
    /* 3322  */ 0xFFFF0202, 
    /* 3323  */ 0xFFFF0808, 
    /* 3324  */ 0xFFFF0E0E, 
    /* 3325  */ 0xFFFF1414, 
    /* 3326  */ 0xFFFF1A1A, 
    /* 3327  */ 0xFFFF2020, 
    /* 3328  */ 0xFFFF2626, 
    /* 3329  */ 0xFFFF2C2C, 
    /* 3330  */ 0xFFFF3232, 
    /* 3331  */ 0xFFFF3939, 
    /* 3332  */ 0xFFFF3F3F, 
    /* 3333  */ 0xFFFF4545, 
    /* 3334  */ 0xFFFF4B4B, 
    /* 3335  */ 0xFFFF5151, 
    /* 3336  */ 0xFFFF5757, 
    /* 3337  */ 0xFFFF5D5D, 
    /* 3338  */ 0xFFFF6363, 
    /* 3339  */ 0xFFFF6A6A, 
    /* 3340  */ 0xFFFF7070, 
    /* 3341  */ 0xFFFF7676, 
    /* 3342  */ 0xFFFF7C7C, 
    /* 3343  */ 0xFFFF8282, 
    /* 3344  */ 0xFFFF8888, 
    /* 3345  */ 0xFFFF8E8E, 
    /* 3346  */ 0xFFFF9494, 
    /* 3347  */ 0xFFFF9B9B, 
    /* 3348  */ 0xFFFFA1A1, 
    /* 3349  */ 0xFFFFA7A7, 
    /* 3350  */ 0xFFFFADAD, 
    /* 3351  */ 0xFFFFB3B3, 
    /* 3352  */ 0xFFFFB9B9, 
    /* 3353  */ 0xFFFFBFBF, 
    /* 3354  */ 0xFFFFC5C5, 
    /* 3355  */ 0xFFFFCBCB, 
    /* 3356  */ 0xFFFFD2D2, 
    /* 3357  */ 0xFFFFD8D8, 
    /* 3358  */ 0xFFFFDEDE, 
    /* 3359  */ 0xFFFFE4E4, 
    /* 3360  */ 0xFFFFEAEA, 
    /* 3361  */ 0xFFFFF0F0, 
    /* 3362  */ 0xFFFFF6F6, 
    /* 3363  */ 0xFFFFFCFC, 
    /* 3364  */ 0xFFFAFFFA, 
    /* 3365  */ 0xFFF4FFF4, 
    /* 3366  */ 0xFFEEFFEE, 
    /* 3367  */ 0xFFE8FFE8, 
    /* 3368  */ 0xFFE2FFE2, 
    /* 3369  */ 0xFFDCFFDC, 
    /* 3370  */ 0xFFD6FFD6, 
    /* 3371  */ 0xFFD0FFD0, 
    /* 3372  */ 0xFFC9FFC9, 
    /* 3373  */ 0xFFC3FFC3, 
    /* 3374  */ 0xFFBDFFBD, 
    /* 3375  */ 0xFFB7FFB7, 
    /* 3376  */ 0xFFB1FFB1, 
    /* 3377  */ 0xFFABFFAB, 
    /* 3378  */ 0xFFA5FFA5, 
    /* 3379  */ 0xFF9FFF9F, 
    /* 3380  */ 0xFF99FF99, 
    /* 3381  */ 0xFF92FF92, 
    /* 3382  */ 0xFF8CFF8C, 
    /* 3383  */ 0xFF86FF86, 
    /* 3384  */ 0xFF80FF80, 
    /* 3385  */ 0xFF7AFF7A, 
    /* 3386  */ 0xFF74FF74, 
    /* 3387  */ 0xFF6EFF6E, 
    /* 3388  */ 0xFF68FF68, 
    /* 3389  */ 0xFF61FF61, 
    /* 3390  */ 0xFF5BFF5B, 
    /* 3391  */ 0xFF55FF55, 
    /* 3392  */ 0xFF4FFF4F, 
    /* 3393  */ 0xFF49FF49, 
    /* 3394  */ 0xFF43FF43, 
    /* 3395  */ 0xFF3DFF3D, 
    /* 3396  */ 0xFF37FF37, 
    /* 3397  */ 0xFF30FF30, 
    /* 3398  */ 0xFF2AFF2A, 
    /* 3399  */ 0xFF24FF24, 
    /* 3400  */ 0xFF1EFF1E, 
    /* 3401  */ 0xFF18FF18, 
    /* 3402  */ 0xFF12FF12, 
    /* 3403  */ 0xFF0CFF0C, 
    /* 3404  */ 0xFF06FF06, 
    /* 3405  */ 0xFF00FF00, 
    /* 3406  */ 0xFF06FF06, 
    /* 3407  */ 0xFF0CFF0C, 
    /* 3408  */ 0xFF12FF12, 
    /* 3409  */ 0xFF18FF18, 
    /* 3410  */ 0xFF1EFF1E, 
    /* 3411  */ 0xFF24FF24, 
    /* 3412  */ 0xFF2AFF2A, 
    /* 3413  */ 0xFF30FF30, 
    /* 3414  */ 0xFF37FF37, 
    /* 3415  */ 0xFF3DFF3D, 
    /* 3416  */ 0xFF43FF43, 
    /* 3417  */ 0xFF49FF49, 
    /* 3418  */ 0xFF4FFF4F, 
    /* 3419  */ 0xFF55FF55, 
    /* 3420  */ 0xFF5BFF5B, 
    /* 3421  */ 0xFF61FF61, 
    /* 3422  */ 0xFF68FF68, 
    /* 3423  */ 0xFF6EFF6E, 
    /* 3424  */ 0xFF74FF74, 
    /* 3425  */ 0xFF7AFF7A, 
    /* 3426  */ 0xFF80FF80, 
    /* 3427  */ 0xFF86FF86, 
    /* 3428  */ 0xFF8CFF8C, 
    /* 3429  */ 0xFF92FF92, 
    /* 3430  */ 0xFF98FF98, 
    /* 3431  */ 0xFF9FFF9F, 
    /* 3432  */ 0xFFA5FFA5, 
    /* 3433  */ 0xFFABFFAB, 
    /* 3434  */ 0xFFB1FFB1, 
    /* 3435  */ 0xFFB7FFB7, 
    /* 3436  */ 0xFFBDFFBD, 
    /* 3437  */ 0xFFC3FFC3, 
    /* 3438  */ 0xFFC9FFC9, 
    /* 3439  */ 0xFFD0FFD0, 
    /* 3440  */ 0xFFD6FFD6, 
    /* 3441  */ 0xFFDCFFDC, 
    /* 3442  */ 0xFFE2FFE2, 
    /* 3443  */ 0xFFE8FFE8, 
    /* 3444  */ 0xFFEEFFEE, 
    /* 3445  */ 0xFFF4FFF4, 
    /* 3446  */ 0xFFFAFFFA, 
    /* 3447  */ 0xFFFFFCFF, 
    /* 3448  */ 0xFFFFF6FF, 
    /* 3449  */ 0xFFFFF0FF, 
    /* 3450  */ 0xFFFFEAFF, 
    /* 3451  */ 0xFFFFE4FF, 
    /* 3452  */ 0xFFFFDEFF, 
    /* 3453  */ 0xFFFFD8FF, 
    /* 3454  */ 0xFFFFD2FF, 
    /* 3455  */ 0xFFFFCCFF, 
    /* 3456  */ 0xFFFFC5FF, 
    /* 3457  */ 0xFFFFBFFF, 
    /* 3458  */ 0xFFFFB9FF, 
    /* 3459  */ 0xFFFFB3FF, 
    /* 3460  */ 0xFFFFADFF, 
    /* 3461  */ 0xFFFFA7FF, 
    /* 3462  */ 0xFFFFA1FF, 
    /* 3463  */ 0xFFFF9BFF, 
    /* 3464  */ 0xFFFF94FF, 
    /* 3465  */ 0xFFFF8EFF, 
    /* 3466  */ 0xFFFF88FF, 
    /* 3467  */ 0xFFFF82FF, 
    /* 3468  */ 0xFFFF7CFF, 
    /* 3469  */ 0xFFFF76FF, 
    /* 3470  */ 0xFFFF70FF, 
    /* 3471  */ 0xFFFF6AFF, 
    /* 3472  */ 0xFFFF63FF, 
    /* 3473  */ 0xFFFF5DFF, 
    /* 3474  */ 0xFFFF57FF, 
    /* 3475  */ 0xFFFF51FF, 
    /* 3476  */ 0xFFFF4BFF, 
    /* 3477  */ 0xFFFF45FF, 
    /* 3478  */ 0xFFFF3FFF, 
    /* 3479  */ 0xFFFF39FF, 
    /* 3480  */ 0xFFFF33FF, 
    /* 3481  */ 0xFFFF2CFF, 
    /* 3482  */ 0xFFFF26FF, 
    /* 3483  */ 0xFFFF20FF, 
    /* 3484  */ 0xFFFF1AFF, 
    /* 3485  */ 0xFFFF14FF, 
    /* 3486  */ 0xFFFF0EFF, 
    /* 3487  */ 0xFFFF08FF, 
    /* 3488  */ 0xFFFF02FF, 
    /* 3489  */ 0xFFFF04FF, 
    /* 3490  */ 0xFFFF0AFF, 
    /* 3491  */ 0xFFFF10FF, 
    /* 3492  */ 0xFFFF16FF, 
    /* 3493  */ 0xFFFF1CFF, 
    /* 3494  */ 0xFFFF22FF, 
    /* 3495  */ 0xFFFF28FF, 
    /* 3496  */ 0xFFFF2EFF, 
    /* 3497  */ 0xFFFF35FF, 
    /* 3498  */ 0xFFFF3BFF, 
    /* 3499  */ 0xFFFF41FF, 
    /* 3500  */ 0xFFFF47FF, 
    /* 3501  */ 0xFFFF4DFF, 
    /* 3502  */ 0xFFFF53FF, 
    /* 3503  */ 0xFFFF59FF, 
    /* 3504  */ 0xFFFF5FFF, 
    /* 3505  */ 0xFFFF65FF, 
    /* 3506  */ 0xFFFF6CFF, 
    /* 3507  */ 0xFFFF72FF, 
    /* 3508  */ 0xFFFF78FF, 
    /* 3509  */ 0xFFFF7EFF, 
    /* 3510  */ 0xFFFF84FF, 
    /* 3511  */ 0xFFFF8AFF, 
    /* 3512  */ 0xFFFF90FF, 
    /* 3513  */ 0xFFFF96FF, 
    /* 3514  */ 0xFFFF9DFF, 
    /* 3515  */ 0xFFFFA3FF, 
    /* 3516  */ 0xFFFFA9FF, 
    /* 3517  */ 0xFFFFAFFF, 
    /* 3518  */ 0xFFFFB5FF, 
    /* 3519  */ 0xFFFFBBFF, 
    /* 3520  */ 0xFFFFC1FF, 
    /* 3521  */ 0xFFFFC7FF, 
    /* 3522  */ 0xFFFFCEFF, 
    /* 3523  */ 0xFFFFD4FF, 
    /* 3524  */ 0xFFFFDAFF, 
    /* 3525  */ 0xFFFFE0FF, 
    /* 3526  */ 0xFFFFE6FF, 
    /* 3527  */ 0xFFFFECFF, 
    /* 3528  */ 0xFFFFF2FF, 
    /* 3529  */ 0xFFFFF8FF, 
    /* 3530  */ 0xFFFFFEFF, 
    /* 3531  */ 0xFFF8FFFF, 
    /* 3532  */ 0xFFF2FFFF, 
    /* 3533  */ 0xFFECFFFF, 
    /* 3534  */ 0xFFE6FFFF, 
    /* 3535  */ 0xFFE0FFFF, 
    /* 3536  */ 0xFFDAFFFF, 
    /* 3537  */ 0xFFD4FFFF, 
    /* 3538  */ 0xFFCEFFFF, 
    /* 3539  */ 0xFFC7FFFF, 
    /* 3540  */ 0xFFC1FFFF, 
    /* 3541  */ 0xFFBBFFFF, 
    /* 3542  */ 0xFFB5FFFF, 
    /* 3543  */ 0xFFAFFFFF, 
    /* 3544  */ 0xFFA9FFFF, 
    /* 3545  */ 0xFFA3FFFF, 
    /* 3546  */ 0xFF9DFFFF, 
    /* 3547  */ 0xFF96FFFF, 
    /* 3548  */ 0xFF90FFFF, 
    /* 3549  */ 0xFF8AFFFF, 
    /* 3550  */ 0xFF84FFFF, 
    /* 3551  */ 0xFF7EFFFF, 
    /* 3552  */ 0xFF78FFFF, 
    /* 3553  */ 0xFF72FFFF, 
    /* 3554  */ 0xFF6CFFFF, 
    /* 3555  */ 0xFF66FFFF, 
    /* 3556  */ 0xFF5FFFFF, 
    /* 3557  */ 0xFF59FFFF, 
    /* 3558  */ 0xFF53FFFF, 
    /* 3559  */ 0xFF4DFFFF, 
    /* 3560  */ 0xFF47FFFF, 
    /* 3561  */ 0xFF41FFFF, 
    /* 3562  */ 0xFF3BFFFF, 
    /* 3563  */ 0xFF35FFFF, 
    /* 3564  */ 0xFF2EFFFF, 
    /* 3565  */ 0xFF28FFFF, 
    /* 3566  */ 0xFF22FFFF, 
    /* 3567  */ 0xFF1CFFFF, 
    /* 3568  */ 0xFF16FFFF, 
    /* 3569  */ 0xFF10FFFF, 
    /* 3570  */ 0xFF0AFFFF, 
    /* 3571  */ 0xFF04FFFF, 
    /* 3572  */ 0xFF02FFFF, 
    /* 3573  */ 0xFF08FFFF, 
    /* 3574  */ 0xFF0EFFFF, 
    /* 3575  */ 0xFF14FFFF, 
    /* 3576  */ 0xFF1AFFFF, 
    /* 3577  */ 0xFF20FFFF, 
    /* 3578  */ 0xFF26FFFF, 
    /* 3579  */ 0xFF2CFFFF, 
    /* 3580  */ 0xFF32FFFF, 
    /* 3581  */ 0xFF39FFFF, 
    /* 3582  */ 0xFF3FFFFF, 
    /* 3583  */ 0xFF45FFFF, 
    /* 3584  */ 0xFF4BFFFF, 
    /* 3585  */ 0xFF51FFFF, 
    /* 3586  */ 0xFF57FFFF, 
    /* 3587  */ 0xFF5DFFFF, 
    /* 3588  */ 0xFF63FFFF, 
    /* 3589  */ 0xFF6AFFFF, 
    /* 3590  */ 0xFF70FFFF, 
    /* 3591  */ 0xFF76FFFF, 
    /* 3592  */ 0xFF7CFFFF, 
    /* 3593  */ 0xFF82FFFF, 
    /* 3594  */ 0xFF88FFFF, 
    /* 3595  */ 0xFF8EFFFF, 
    /* 3596  */ 0xFF94FFFF, 
    /* 3597  */ 0xFF9BFFFF, 
    /* 3598  */ 0xFFA1FFFF, 
    /* 3599  */ 0xFFA7FFFF, 
    /* 3600  */ 0xFFADFFFF, 
    /* 3601  */ 0xFFB3FFFF, 
    /* 3602  */ 0xFFB9FFFF, 
    /* 3603  */ 0xFFBFFFFF, 
    /* 3604  */ 0xFFC5FFFF, 
    /* 3605  */ 0xFFCBFFFF, 
    /* 3606  */ 0xFFD2FFFF, 
    /* 3607  */ 0xFFD8FFFF, 
    /* 3608  */ 0xFFDEFFFF, 
    /* 3609  */ 0xFFE4FFFF, 
    /* 3610  */ 0xFFEAFFFF, 
    /* 3611  */ 0xFFF0FFFF, 
    /* 3612  */ 0xFFF6FFFF, 
    /* 3613  */ 0xFFFCFFFF, 
    /* 3614  */ 0xFFFFFFFA, 
    /* 3615  */ 0xFFFFFFF4, 
    /* 3616  */ 0xFFFFFFEE, 
    /* 3617  */ 0xFFFFFFE8, 
    /* 3618  */ 0xFFFFFFE2, 
    /* 3619  */ 0xFFFFFFDC, 
    /* 3620  */ 0xFFFFFFD6, 
    /* 3621  */ 0xFFFFFFD0, 
    /* 3622  */ 0xFFFFFFC9, 
    /* 3623  */ 0xFFFFFFC3, 
    /* 3624  */ 0xFFFFFFBD, 
    /* 3625  */ 0xFFFFFFB7, 
    /* 3626  */ 0xFFFFFFB1, 
    /* 3627  */ 0xFFFFFFAB, 
    /* 3628  */ 0xFFFFFFA5, 
    /* 3629  */ 0xFFFFFF9F, 
    /* 3630  */ 0xFFFFFF99, 
    /* 3631  */ 0xFFFFFF92, 
    /* 3632  */ 0xFFFFFF8C, 
    /* 3633  */ 0xFFFFFF86, 
    /* 3634  */ 0xFFFFFF80, 
    /* 3635  */ 0xFFFFFF7A, 
    /* 3636  */ 0xFFFFFF74, 
    /* 3637  */ 0xFFFFFF6E, 
    /* 3638  */ 0xFFFFFF68, 
    /* 3639  */ 0xFFFFFF61, 
    /* 3640  */ 0xFFFFFF5B, 
    /* 3641  */ 0xFFFFFF55, 
    /* 3642  */ 0xFFFFFF4F, 
    /* 3643  */ 0xFFFFFF49, 
    /* 3644  */ 0xFFFFFF43, 
    /* 3645  */ 0xFFFFFF3D, 
    /* 3646  */ 0xFFFFFF37, 
    /* 3647  */ 0xFFFFFF30, 
    /* 3648  */ 0xFFFFFF2A, 
    /* 3649  */ 0xFFFFFF24, 
    /* 3650  */ 0xFFFFFF1E, 
    /* 3651  */ 0xFFFFFF18, 
    /* 3652  */ 0xFFFFFF12, 
    /* 3653  */ 0xFFFFFF0C, 
    /* 3654  */ 0xFFFFFF06, 
    /* 3655  */ 0xFFFFFF00, 
    /* 3656  */ 0xFFFFFF06, 
    /* 3657  */ 0xFFFFFF0C, 
    /* 3658  */ 0xFFFFFF12, 
    /* 3659  */ 0xFFFFFF18, 
    /* 3660  */ 0xFFFFFF1E, 
    /* 3661  */ 0xFFFFFF24, 
    /* 3662  */ 0xFFFFFF2A, 
    /* 3663  */ 0xFFFFFF30, 
    /* 3664  */ 0xFFFFFF37, 
    /* 3665  */ 0xFFFFFF3D, 
    /* 3666  */ 0xFFFFFF43, 
    /* 3667  */ 0xFFFFFF49, 
    /* 3668  */ 0xFFFFFF4F, 
    /* 3669  */ 0xFFFFFF55, 
    /* 3670  */ 0xFFFFFF5B, 
    /* 3671  */ 0xFFFFFF61, 
    /* 3672  */ 0xFFFFFF68, 
    /* 3673  */ 0xFFFFFF6E, 
    /* 3674  */ 0xFFFFFF74, 
    /* 3675  */ 0xFFFFFF7A, 
    /* 3676  */ 0xFFFFFF80, 
    /* 3677  */ 0xFFFFFF86, 
    /* 3678  */ 0xFFFFFF8C, 
    /* 3679  */ 0xFFFFFF92, 
    /* 3680  */ 0xFFFFFF98, 
    /* 3681  */ 0xFFFFFF9F, 
    /* 3682  */ 0xFFFFFFA5, 
    /* 3683  */ 0xFFFFFFAB, 
    /* 3684  */ 0xFFFFFFB1, 
    /* 3685  */ 0xFFFFFFB7, 
    /* 3686  */ 0xFFFFFFBD, 
    /* 3687  */ 0xFFFFFFC3, 
    /* 3688  */ 0xFFFFFFC9, 
    /* 3689  */ 0xFFFFFFD0, 
    /* 3690  */ 0xFFFFFFD6, 
    /* 3691  */ 0xFFFFFFDC, 
    /* 3692  */ 0xFFFFFFE2, 
    /* 3693  */ 0xFFFFFFE8, 
    /* 3694  */ 0xFFFFFFEE, 
    /* 3695  */ 0xFFFFFFF4, 
    /* 3696  */ 0xFFFFFFFA, 
    /* 3697  */ 0xFFFCFFFC, 
    /* 3698  */ 0xFFF6FFF6, 
    /* 3699  */ 0xFFF0FFF0, 
    /* 3700  */ 0xFFEAFFEA, 
    /* 3701  */ 0xFFE4FFE4, 
    /* 3702  */ 0xFFDEFFDE, 
    /* 3703  */ 0xFFD8FFD8, 
    /* 3704  */ 0xFFD2FFD2, 
    /* 3705  */ 0xFFCCFFCC, 
    /* 3706  */ 0xFFC5FFC5, 
    /* 3707  */ 0xFFBFFFBF, 
    /* 3708  */ 0xFFB9FFB9, 
    /* 3709  */ 0xFFB3FFB3, 
    /* 3710  */ 0xFFADFFAD, 
    /* 3711  */ 0xFFA7FFA7, 
    /* 3712  */ 0xFFA1FFA1, 
    /* 3713  */ 0xFF9BFF9B, 
    /* 3714  */ 0xFF94FF94, 
    /* 3715  */ 0xFF8EFF8E, 
    /* 3716  */ 0xFF88FF88, 
    /* 3717  */ 0xFF82FF82, 
    /* 3718  */ 0xFF7CFF7C, 
    /* 3719  */ 0xFF76FF76, 
    /* 3720  */ 0xFF70FF70, 
    /* 3721  */ 0xFF6AFF6A, 
    /* 3722  */ 0xFF63FF63, 
    /* 3723  */ 0xFF5DFF5D, 
    /* 3724  */ 0xFF57FF57, 
    /* 3725  */ 0xFF51FF51, 
    /* 3726  */ 0xFF4BFF4B, 
    /* 3727  */ 0xFF45FF45, 
    /* 3728  */ 0xFF3FFF3F, 
    /* 3729  */ 0xFF39FF39, 
    /* 3730  */ 0xFF33FF33, 
    /* 3731  */ 0xFF2CFF2C, 
    /* 3732  */ 0xFF26FF26, 
    /* 3733  */ 0xFF20FF20, 
    /* 3734  */ 0xFF1AFF1A, 
    /* 3735  */ 0xFF14FF14, 
    /* 3736  */ 0xFF0EFF0E, 
    /* 3737  */ 0xFF08FF08, 
    /* 3738  */ 0xFF02FF02, 
    /* 3739  */ 0xFF04FF04, 
    /* 3740  */ 0xFF0AFF0A, 
    /* 3741  */ 0xFF10FF10, 
    /* 3742  */ 0xFF16FF16, 
    /* 3743  */ 0xFF1CFF1C, 
    /* 3744  */ 0xFF22FF22, 
    /* 3745  */ 0xFF28FF28, 
    /* 3746  */ 0xFF2EFF2E, 
    /* 3747  */ 0xFF35FF35, 
    /* 3748  */ 0xFF3BFF3B, 
    /* 3749  */ 0xFF41FF41, 
    /* 3750  */ 0xFF47FF47, 
    /* 3751  */ 0xFF4DFF4D, 
    /* 3752  */ 0xFF53FF53, 
    /* 3753  */ 0xFF59FF59, 
    /* 3754  */ 0xFF5FFF5F, 
    /* 3755  */ 0xFF65FF65, 
    /* 3756  */ 0xFF6CFF6C, 
    /* 3757  */ 0xFF72FF72, 
    /* 3758  */ 0xFF78FF78, 
    /* 3759  */ 0xFF7EFF7E, 
    /* 3760  */ 0xFF84FF84, 
    /* 3761  */ 0xFF8AFF8A, 
    /* 3762  */ 0xFF90FF90, 
    /* 3763  */ 0xFF96FF96, 
    /* 3764  */ 0xFF9DFF9D, 
    /* 3765  */ 0xFFA3FFA3, 
    /* 3766  */ 0xFFA9FFA9, 
    /* 3767  */ 0xFFAFFFAF, 
    /* 3768  */ 0xFFB5FFB5, 
    /* 3769  */ 0xFFBBFFBB, 
    /* 3770  */ 0xFFC1FFC1, 
    /* 3771  */ 0xFFC7FFC7, 
    /* 3772  */ 0xFFCEFFCE, 
    /* 3773  */ 0xFFD4FFD4, 
    /* 3774  */ 0xFFDAFFDA, 
    /* 3775  */ 0xFFE0FFE0, 
    /* 3776  */ 0xFFE6FFE6, 
    /* 3777  */ 0xFFECFFEC, 
    /* 3778  */ 0xFFF2FFF2, 
    /* 3779  */ 0xFFF8FFF8, 
    /* 3780  */ 0xFFFEFFFE, 
    /* 3781  */ 0xFFF8F8FF, 
    /* 3782  */ 0xFFF2F2FF, 
    /* 3783  */ 0xFFECECFF, 
    /* 3784  */ 0xFFE6E6FF, 
    /* 3785  */ 0xFFE0E0FF, 
    /* 3786  */ 0xFFDADAFF, 
    /* 3787  */ 0xFFD4D4FF, 
    /* 3788  */ 0xFFCECEFF, 
    /* 3789  */ 0xFFC7C7FF, 
    /* 3790  */ 0xFFC1C1FF, 
    /* 3791  */ 0xFFBBBBFF, 
    /* 3792  */ 0xFFB5B5FF, 
    /* 3793  */ 0xFFAFAFFF, 
    /* 3794  */ 0xFFA9A9FF, 
    /* 3795  */ 0xFFA3A3FF, 
    /* 3796  */ 0xFF9D9DFF, 
    /* 3797  */ 0xFF9696FF, 
    /* 3798  */ 0xFF9090FF, 
    /* 3799  */ 0xFF8A8AFF, 
    /* 3800  */ 0xFF8484FF, 
    /* 3801  */ 0xFF7E7EFF, 
    /* 3802  */ 0xFF7878FF, 
    /* 3803  */ 0xFF7272FF, 
    /* 3804  */ 0xFF6C6CFF, 
    /* 3805  */ 0xFF6666FF, 
    /* 3806  */ 0xFF5F5FFF, 
    /* 3807  */ 0xFF5959FF, 
    /* 3808  */ 0xFF5353FF, 
    /* 3809  */ 0xFF4D4DFF, 
    /* 3810  */ 0xFF4747FF, 
    /* 3811  */ 0xFF4141FF, 
    /* 3812  */ 0xFF3B3BFF, 
    /* 3813  */ 0xFF3535FF, 
    /* 3814  */ 0xFF2E2EFF, 
    /* 3815  */ 0xFF2828FF, 
    /* 3816  */ 0xFF2222FF, 
    /* 3817  */ 0xFF1C1CFF, 
    /* 3818  */ 0xFF1616FF, 
    /* 3819  */ 0xFF1010FF, 
    /* 3820  */ 0xFF0A0AFF, 
    /* 3821  */ 0xFF0404FF, 
    /* 3822  */ 0xFF0202FF, 
    /* 3823  */ 0xFF0808FF, 
    /* 3824  */ 0xFF0E0EFF, 
    /* 3825  */ 0xFF1414FF, 
    /* 3826  */ 0xFF1A1AFF, 
    /* 3827  */ 0xFF2020FF, 
    /* 3828  */ 0xFF2626FF, 
    /* 3829  */ 0xFF2C2CFF, 
    /* 3830  */ 0xFF3232FF, 
    /* 3831  */ 0xFF3939FF, 
    /* 3832  */ 0xFF3F3FFF, 
    /* 3833  */ 0xFF4545FF, 
    /* 3834  */ 0xFF4B4BFF, 
    /* 3835  */ 0xFF5151FF, 
    /* 3836  */ 0xFF5757FF, 
    /* 3837  */ 0xFF5D5DFF, 
    /* 3838  */ 0xFF6363FF, 
    /* 3839  */ 0xFF6A6AFF, 
    /* 3840  */ 0xFF7070FF, 
    /* 3841  */ 0xFF7676FF, 
    /* 3842  */ 0xFF7C7CFF, 
    /* 3843  */ 0xFF8282FF, 
    /* 3844  */ 0xFF8888FF, 
    /* 3845  */ 0xFF8E8EFF, 
    /* 3846  */ 0xFF9494FF, 
    /* 3847  */ 0xFF9B9BFF, 
    /* 3848  */ 0xFFA1A1FF, 
    /* 3849  */ 0xFFA7A7FF, 
    /* 3850  */ 0xFFADADFF, 
    /* 3851  */ 0xFFB3B3FF, 
    /* 3852  */ 0xFFB9B9FF, 
    /* 3853  */ 0xFFBFBFFF, 
    /* 3854  */ 0xFFC5C5FF, 
    /* 3855  */ 0xFFCBCBFF, 
    /* 3856  */ 0xFFD2D2FF, 
    /* 3857  */ 0xFFD8D8FF, 
    /* 3858  */ 0xFFDEDEFF, 
    /* 3859  */ 0xFFE4E4FF, 
    /* 3860  */ 0xFFEAEAFF, 
    /* 3861  */ 0xFFF0F0FF, 
    /* 3862  */ 0xFFF6F6FF, 
    /* 3863  */ 0xFFFCFCFF, 
    /* 3864  */ 0xFFFFFAFF, 
    /* 3865  */ 0xFFFFF4FF, 
    /* 3866  */ 0xFFFFEEFF, 
    /* 3867  */ 0xFFFFE8FF, 
    /* 3868  */ 0xFFFFE2FF, 
    /* 3869  */ 0xFFFFDCFF, 
    /* 3870  */ 0xFFFFD6FF, 
    /* 3871  */ 0xFFFFD0FF, 
    /* 3872  */ 0xFFFFC9FF, 
    /* 3873  */ 0xFFFFC3FF, 
    /* 3874  */ 0xFFFFBDFF, 
    /* 3875  */ 0xFFFFB7FF, 
    /* 3876  */ 0xFFFFB1FF, 
    /* 3877  */ 0xFFFFABFF, 
    /* 3878  */ 0xFFFFA5FF, 
    /* 3879  */ 0xFFFF9FFF, 
    /* 3880  */ 0xFFFF99FF, 
    /* 3881  */ 0xFFFF92FF, 
    /* 3882  */ 0xFFFF8CFF, 
    /* 3883  */ 0xFFFF86FF, 
    /* 3884  */ 0xFFFF80FF, 
    /* 3885  */ 0xFFFF7AFF, 
    /* 3886  */ 0xFFFF74FF, 
    /* 3887  */ 0xFFFF6EFF, 
    /* 3888  */ 0xFFFF68FF, 
    /* 3889  */ 0xFFFF61FF, 
    /* 3890  */ 0xFFFF5BFF, 
    /* 3891  */ 0xFFFF55FF, 
    /* 3892  */ 0xFFFF4FFF, 
    /* 3893  */ 0xFFFF49FF, 
    /* 3894  */ 0xFFFF43FF, 
    /* 3895  */ 0xFFFF3DFF, 
    /* 3896  */ 0xFFFF37FF, 
    /* 3897  */ 0xFFFF30FF, 
    /* 3898  */ 0xFFFF2AFF, 
    /* 3899  */ 0xFFFF24FF, 
    /* 3900  */ 0xFFFF1EFF, 
    /* 3901  */ 0xFFFF18FF, 
    /* 3902  */ 0xFFFF12FF, 
    /* 3903  */ 0xFFFF0CFF, 
    /* 3904  */ 0xFFFF06FF, 
    /* 3905  */ 0xFFFF00FF, 
    /* 3906  */ 0xFFFF06FF, 
    /* 3907  */ 0xFFFF0CFF, 
    /* 3908  */ 0xFFFF12FF, 
    /* 3909  */ 0xFFFF18FF, 
    /* 3910  */ 0xFFFF1EFF, 
    /* 3911  */ 0xFFFF24FF, 
    /* 3912  */ 0xFFFF2AFF, 
    /* 3913  */ 0xFFFF30FF, 
    /* 3914  */ 0xFFFF37FF, 
    /* 3915  */ 0xFFFF3DFF, 
    /* 3916  */ 0xFFFF43FF, 
    /* 3917  */ 0xFFFF49FF, 
    /* 3918  */ 0xFFFF4FFF, 
    /* 3919  */ 0xFFFF55FF, 
    /* 3920  */ 0xFFFF5BFF, 
    /* 3921  */ 0xFFFF61FF, 
    /* 3922  */ 0xFFFF68FF, 
    /* 3923  */ 0xFFFF6EFF, 
    /* 3924  */ 0xFFFF74FF, 
    /* 3925  */ 0xFFFF7AFF, 
    /* 3926  */ 0xFFFF80FF, 
    /* 3927  */ 0xFFFF86FF, 
    /* 3928  */ 0xFFFF8CFF, 
    /* 3929  */ 0xFFFF92FF, 
    /* 3930  */ 0xFFFF98FF, 
    /* 3931  */ 0xFFFF9FFF, 
    /* 3932  */ 0xFFFFA5FF, 
    /* 3933  */ 0xFFFFABFF, 
    /* 3934  */ 0xFFFFB1FF, 
    /* 3935  */ 0xFFFFB7FF, 
    /* 3936  */ 0xFFFFBDFF, 
    /* 3937  */ 0xFFFFC3FF, 
    /* 3938  */ 0xFFFFC9FF, 
    /* 3939  */ 0xFFFFD0FF, 
    /* 3940  */ 0xFFFFD6FF, 
    /* 3941  */ 0xFFFFDCFF, 
    /* 3942  */ 0xFFFFE2FF, 
    /* 3943  */ 0xFFFFE8FF, 
    /* 3944  */ 0xFFFFEEFF, 
    /* 3945  */ 0xFFFFF4FF, 
    /* 3946  */ 0xFFFFFAFF, 
    /* 3947  */ 0xFFFFFFFC, 
    /* 3948  */ 0xFFFFFFF6, 
    /* 3949  */ 0xFFFFFFF0, 
    /* 3950  */ 0xFFFFFFEA, 
    /* 3951  */ 0xFFFFFFE4, 
    /* 3952  */ 0xFFFFFFDE, 
    /* 3953  */ 0xFFFFFFD8, 
    /* 3954  */ 0xFFFFFFD2, 
    /* 3955  */ 0xFFFFFFCC, 
    /* 3956  */ 0xFFFFFFC5, 
    /* 3957  */ 0xFFFFFFBF, 
    /* 3958  */ 0xFFFFFFB9, 
    /* 3959  */ 0xFFFFFFB3, 
    /* 3960  */ 0xFFFFFFAD, 
    /* 3961  */ 0xFFFFFFA7, 
    /* 3962  */ 0xFFFFFFA1, 
    /* 3963  */ 0xFFFFFF9B, 
    /* 3964  */ 0xFFFFFF94, 
    /* 3965  */ 0xFFFFFF8E, 
    /* 3966  */ 0xFFFFFF88, 
    /* 3967  */ 0xFFFFFF82, 
    /* 3968  */ 0xFFFFFF7C, 
    /* 3969  */ 0xFFFFFF76, 
    /* 3970  */ 0xFFFFFF70, 
    /* 3971  */ 0xFFFFFF6A, 
    /* 3972  */ 0xFFFFFF63, 
    /* 3973  */ 0xFFFFFF5D, 
    /* 3974  */ 0xFFFFFF57, 
    /* 3975  */ 0xFFFFFF51, 
    /* 3976  */ 0xFFFFFF4B, 
    /* 3977  */ 0xFFFFFF45, 
    /* 3978  */ 0xFFFFFF3F, 
    /* 3979  */ 0xFFFFFF39, 
    /* 3980  */ 0xFFFFFF33, 
    /* 3981  */ 0xFFFFFF2C, 
    /* 3982  */ 0xFFFFFF26, 
    /* 3983  */ 0xFFFFFF20, 
    /* 3984  */ 0xFFFFFF1A, 
    /* 3985  */ 0xFFFFFF14, 
    /* 3986  */ 0xFFFFFF0E, 
    /* 3987  */ 0xFFFFFF08, 
    /* 3988  */ 0xFFFFFF02, 
    /* 3989  */ 0xFFFFFF04, 
    /* 3990  */ 0xFFFFFF0A, 
    /* 3991  */ 0xFFFFFF10, 
    /* 3992  */ 0xFFFFFF16, 
    /* 3993  */ 0xFFFFFF1C, 
    /* 3994  */ 0xFFFFFF22, 
    /* 3995  */ 0xFFFFFF28, 
    /* 3996  */ 0xFFFFFF2E, 
    /* 3997  */ 0xFFFFFF35, 
    /* 3998  */ 0xFFFFFF3B, 
    /* 3999  */ 0xFFFFFF41, 
    /* 4000  */ 0xFFFFFF47, 
    /* 4001  */ 0xFFFFFF4D, 
    /* 4002  */ 0xFFFFFF53, 
    /* 4003  */ 0xFFFFFF59, 
    /* 4004  */ 0xFFFFFF5F, 
    /* 4005  */ 0xFFFFFF65, 
    /* 4006  */ 0xFFFFFF6C, 
    /* 4007  */ 0xFFFFFF72, 
    /* 4008  */ 0xFFFFFF78, 
    /* 4009  */ 0xFFFFFF7E, 
    /* 4010  */ 0xFFFFFF84, 
    /* 4011  */ 0xFFFFFF8A, 
    /* 4012  */ 0xFFFFFF90, 
    /* 4013  */ 0xFFFFFF96, 
    /* 4014  */ 0xFFFFFF9D, 
    /* 4015  */ 0xFFFFFFA3, 
    /* 4016  */ 0xFFFFFFA9, 
    /* 4017  */ 0xFFFFFFAF, 
    /* 4018  */ 0xFFFFFFB5, 
    /* 4019  */ 0xFFFFFFBB, 
    /* 4020  */ 0xFFFFFFC1, 
    /* 4021  */ 0xFFFFFFC7, 
    /* 4022  */ 0xFFFFFFCE, 
    /* 4023  */ 0xFFFFFFD4, 
    /* 4024  */ 0xFFFFFFDA, 
    /* 4025  */ 0xFFFFFFE0, 
    /* 4026  */ 0xFFFFFFE6, 
    /* 4027  */ 0xFFFFFFEC, 
    /* 4028  */ 0xFFFFFFF2, 
    /* 4029  */ 0xFFFFFFF8, 
    /* 4030  */ 0xFFFFFFFE, 
    /* 4031  */ 0xFFFFF8F8, 
    /* 4032  */ 0xFFFFF2F2, 
    /* 4033  */ 0xFFFFECEC, 
    /* 4034  */ 0xFFFFE6E6, 
    /* 4035  */ 0xFFFFE0E0, 
    /* 4036  */ 0xFFFFDADA, 
    /* 4037  */ 0xFFFFD4D4, 
    /* 4038  */ 0xFFFFCECE, 
    /* 4039  */ 0xFFFFC7C7, 
    /* 4040  */ 0xFFFFC1C1, 
    /* 4041  */ 0xFFFFBBBB, 
    /* 4042  */ 0xFFFFB5B5, 
    /* 4043  */ 0xFFFFAFAF, 
    /* 4044  */ 0xFFFFA9A9, 
    /* 4045  */ 0xFFFFA3A3, 
    /* 4046  */ 0xFFFF9D9D, 
    /* 4047  */ 0xFFFF9696, 
    /* 4048  */ 0xFFFF9090, 
    /* 4049  */ 0xFFFF8A8A, 
    /* 4050  */ 0xFFFF8484, 
    /* 4051  */ 0xFFFF7E7E, 
    /* 4052  */ 0xFFFF7878, 
    /* 4053  */ 0xFFFF7272, 
    /* 4054  */ 0xFFFF6C6C, 
    /* 4055  */ 0xFFFF6666, 
    /* 4056  */ 0xFFFF5F5F, 
    /* 4057  */ 0xFFFF5959, 
    /* 4058  */ 0xFFFF5353, 
    /* 4059  */ 0xFFFF4D4D, 
    /* 4060  */ 0xFFFF4747, 
    /* 4061  */ 0xFFFF4141, 
    /* 4062  */ 0xFFFF3B3B, 
    /* 4063  */ 0xFFFF3535, 
    /* 4064  */ 0xFFFF2E2E, 
    /* 4065  */ 0xFFFF2828, 
    /* 4066  */ 0xFFFF2222, 
    /* 4067  */ 0xFFFF1C1C, 
    /* 4068  */ 0xFFFF1616, 
    /* 4069  */ 0xFFFF1010, 
    /* 4070  */ 0xFFFF0A0A, 
    /* 4071  */ 0xFFFF0404, 
    /* 4072  */ 0xFFFF0202, 
    /* 4073  */ 0xFFFF0808, 
    /* 4074  */ 0xFFFF0E0E, 
    /* 4075  */ 0xFFFF1414, 
    /* 4076  */ 0xFFFF1A1A, 
    /* 4077  */ 0xFFFF2020, 
    /* 4078  */ 0xFFFF2626, 
    /* 4079  */ 0xFFFF2C2C, 
    /* 4080  */ 0xFFFF3232, 
    /* 4081  */ 0xFFFF3939, 
    /* 4082  */ 0xFFFF3F3F, 
    /* 4083  */ 0xFFFF4545, 
    /* 4084  */ 0xFFFF4B4B, 
    /* 4085  */ 0xFFFF5151, 
    /* 4086  */ 0xFFFF5757, 
    /* 4087  */ 0xFFFF5D5D, 
    /* 4088  */ 0xFFFF6363, 
    /* 4089  */ 0xFFFF6A6A, 
    /* 4090  */ 0xFFFF7070, 
    /* 4091  */ 0xFFFF7676, 
    /* 4092  */ 0xFFFF7C7C, 
    /* 4093  */ 0xFFFF8282, 
    /* 4094  */ 0xFFFF8888, 
    /* 4095  */ 0xFFFF8E8E, 
    /* 4096  */ 0xFFFF9494, 
    /* 4097  */ 0xFFFF9B9B, 
    /* 4098  */ 0xFFFFA1A1, 
    /* 4099  */ 0xFFFFA7A7, 
    /* 4100  */ 0xFFFFADAD, 
    /* 4101  */ 0xFFFFB3B3, 
    /* 4102  */ 0xFFFFB9B9, 
    /* 4103  */ 0xFFFFBFBF, 
    /* 4104  */ 0xFFFFC5C5, 
    /* 4105  */ 0xFFFFCBCB, 
    /* 4106  */ 0xFFFFD2D2, 
    /* 4107  */ 0xFFFFD8D8, 
    /* 4108  */ 0xFFFFDEDE, 
    /* 4109  */ 0xFFFFE4E4, 
    /* 4110  */ 0xFFFFEAEA, 
    /* 4111  */ 0xFFFFF0F0, 
    /* 4112  */ 0xFFFFF6F6, 
    /* 4113  */ 0xFFFFFCFC, 
    /* 4114  */ 0xFFFAFFFF, 
    /* 4115  */ 0xFFF4FFFF, 
    /* 4116  */ 0xFFEEFFFF, 
    /* 4117  */ 0xFFE8FFFF, 
    /* 4118  */ 0xFFE2FFFF, 
    /* 4119  */ 0xFFDCFFFF, 
    /* 4120  */ 0xFFD6FFFF, 
    /* 4121  */ 0xFFD0FFFF, 
    /* 4122  */ 0xFFC9FFFF, 
    /* 4123  */ 0xFFC3FFFF, 
    /* 4124  */ 0xFFBDFFFF, 
    /* 4125  */ 0xFFB7FFFF, 
    /* 4126  */ 0xFFB1FFFF, 
    /* 4127  */ 0xFFABFFFF, 
    /* 4128  */ 0xFFA5FFFF, 
    /* 4129  */ 0xFF9FFFFF, 
    /* 4130  */ 0xFF99FFFF, 
    /* 4131  */ 0xFF92FFFF, 
    /* 4132  */ 0xFF8CFFFF, 
    /* 4133  */ 0xFF86FFFF, 
    /* 4134  */ 0xFF80FFFF, 
    /* 4135  */ 0xFF7AFFFF, 
    /* 4136  */ 0xFF74FFFF, 
    /* 4137  */ 0xFF6EFFFF, 
    /* 4138  */ 0xFF68FFFF, 
    /* 4139  */ 0xFF61FFFF, 
    /* 4140  */ 0xFF5BFFFF, 
    /* 4141  */ 0xFF55FFFF, 
    /* 4142  */ 0xFF4FFFFF, 
    /* 4143  */ 0xFF49FFFF, 
    /* 4144  */ 0xFF43FFFF, 
    /* 4145  */ 0xFF3DFFFF, 
    /* 4146  */ 0xFF37FFFF, 
    /* 4147  */ 0xFF30FFFF, 
    /* 4148  */ 0xFF2AFFFF, 
    /* 4149  */ 0xFF24FFFF, 
    /* 4150  */ 0xFF1EFFFF, 
    /* 4151  */ 0xFF18FFFF, 
    /* 4152  */ 0xFF12FFFF, 
    /* 4153  */ 0xFF0CFFFF, 
    /* 4154  */ 0xFF06FFFF, 
    /* 4155  */ 0xFF191999, 
    /* 4156  */ 0xFF000000, 
    /* 4157  */ 0xFF020202, 
    /* 4158  */ 0xFF050505, 
    /* 4159  */ 0xFF070707, 
    /* 4160  */ 0xFF0A0A0A, 
    /* 4161  */ 0xFF0C0C0C, 
    /* 4162  */ 0xFF0F0F0F, 
    /* 4163  */ 0xFF121212, 
    /* 4164  */ 0xFF141414, 
    /* 4165  */ 0xFF171717, 
    /* 4166  */ 0xFF191919, 
    /* 4167  */ 0xFF1C1C1C, 
    /* 4168  */ 0xFF1E1E1E, 
    /* 4169  */ 0xFF212121, 
    /* 4170  */ 0xFF242424, 
    /* 4171  */ 0xFF262626, 
    /* 4172  */ 0xFF292929, 
    /* 4173  */ 0xFF2B2B2B, 
    /* 4174  */ 0xFF2E2E2E, 
    /* 4175  */ 0xFF303030, 
    /* 4176  */ 0xFF333333, 
    /* 4177  */ 0xFF363636, 
    /* 4178  */ 0xFF383838, 
    /* 4179  */ 0xFF3B3B3B, 
    /* 4180  */ 0xFF3D3D3D, 
    /* 4181  */ 0xFF404040, 
    /* 4182  */ 0xFF424242, 
    /* 4183  */ 0xFF454545, 
    /* 4184  */ 0xFF484848, 
    /* 4185  */ 0xFF4A4A4A, 
    /* 4186  */ 0xFF4D4D4D, 
    /* 4187  */ 0xFF4F4F4F, 
    /* 4188  */ 0xFF525252, 
    /* 4189  */ 0xFF555555, 
    /* 4190  */ 0xFF575757, 
    /* 4191  */ 0xFF5A5A5A, 
    /* 4192  */ 0xFF5C5C5C, 
    /* 4193  */ 0xFF5F5F5F, 
    /* 4194  */ 0xFF616161, 
    /* 4195  */ 0xFF646464, 
    /* 4196  */ 0xFF676767, 
    /* 4197  */ 0xFF696969, 
    /* 4198  */ 0xFF6C6C6C, 
    /* 4199  */ 0xFF6E6E6E, 
    /* 4200  */ 0xFF717171, 
    /* 4201  */ 0xFF737373, 
    /* 4202  */ 0xFF767676, 
    /* 4203  */ 0xFF797979, 
    /* 4204  */ 0xFF7B7B7B, 
    /* 4205  */ 0xFF7E7E7E, 
    /* 4206  */ 0xFF808080, 
    /* 4207  */ 0xFF838383, 
    /* 4208  */ 0xFF858585, 
    /* 4209  */ 0xFF888888, 
    /* 4210  */ 0xFF8B8B8B, 
    /* 4211  */ 0xFF8D8D8D, 
    /* 4212  */ 0xFF909090, 
    /* 4213  */ 0xFF929292, 
    /* 4214  */ 0xFF959595, 
    /* 4215  */ 0xFF979797, 
    /* 4216  */ 0xFF9A9A9A, 
    /* 4217  */ 0xFF9D9D9D, 
    /* 4218  */ 0xFF9F9F9F, 
    /* 4219  */ 0xFFA2A2A2, 
    /* 4220  */ 0xFFA4A4A4, 
    /* 4221  */ 0xFFA7A7A7, 
    /* 4222  */ 0xFFAAAAAA, 
    /* 4223  */ 0xFFACACAC, 
    /* 4224  */ 0xFFAFAFAF, 
    /* 4225  */ 0xFFB1B1B1, 
    /* 4226  */ 0xFFB4B4B4, 
    /* 4227  */ 0xFFB6B6B6, 
    /* 4228  */ 0xFFB9B9B9, 
    /* 4229  */ 0xFFBCBCBC, 
    /* 4230  */ 0xFFBEBEBE, 
    /* 4231  */ 0xFFC1C1C1, 
    /* 4232  */ 0xFFC3C3C3, 
    /* 4233  */ 0xFFC6C6C6, 
    /* 4234  */ 0xFFC8C8C8, 
    /* 4235  */ 0xFFCBCBCB, 
    /* 4236  */ 0xFFCECECE, 
    /* 4237  */ 0xFFD0D0D0, 
    /* 4238  */ 0xFFD3D3D3, 
    /* 4239  */ 0xFFD5D5D5, 
    /* 4240  */ 0xFFD8D8D8, 
    /* 4241  */ 0xFFDADADA, 
    /* 4242  */ 0xFFDDDDDD, 
    /* 4243  */ 0xFFE0E0E0, 
    /* 4244  */ 0xFFE2E2E2, 
    /* 4245  */ 0xFFE5E5E5, 
    /* 4246  */ 0xFFE7E7E7, 
    /* 4247  */ 0xFFEAEAEA, 
    /* 4248  */ 0xFFECECEC, 
    /* 4249  */ 0xFFEFEFEF, 
    /* 4250  */ 0xFFF2F2F2, 
    /* 4251  */ 0xFFF4F4F4, 
    /* 4252  */ 0xFFF7F7F7, 
    /* 4253  */ 0xFFF9F9F9, 
    /* 4254  */ 0xFFFCFCFC, 
    /* 4255  */ 0xFFFFFFFF, 
    /* 4256  */ 0xFFFF00FF, 
    /* 4257  */ 0xFFFD00FF, 
    /* 4258  */ 0xFFFC00FF, 
    /* 4259  */ 0xFFFA00FF, 
    /* 4260  */ 0xFFF900FF, 
    /* 4261  */ 0xFFF700FF, 
    /* 4262  */ 0xFFF600FF, 
    /* 4263  */ 0xFFF500FF, 
    /* 4264  */ 0xFFF300FF, 
    /* 4265  */ 0xFFF200FF, 
    /* 4266  */ 0xFFF000FF, 
    /* 4267  */ 0xFFEF00FF, 
    /* 4268  */ 0xFFED00FF, 
    /* 4269  */ 0xFFEC00FF, 
    /* 4270  */ 0xFFEB00FF, 
    /* 4271  */ 0xFFE900FF, 
    /* 4272  */ 0xFFE800FF, 
    /* 4273  */ 0xFFE600FF, 
    /* 4274  */ 0xFFE500FF, 
    /* 4275  */ 0xFFE300FF, 
    /* 4276  */ 0xFFE200FF, 
    /* 4277  */ 0xFFE100FF, 
    /* 4278  */ 0xFFDF00FF, 
    /* 4279  */ 0xFFDE00FF, 
    /* 4280  */ 0xFFDC00FF, 
    /* 4281  */ 0xFFDB00FF, 
    /* 4282  */ 0xFFD900FF, 
    /* 4283  */ 0xFFD800FF, 
    /* 4284  */ 0xFFD700FF, 
    /* 4285  */ 0xFFD500FF, 
    /* 4286  */ 0xFFD400FF, 
    /* 4287  */ 0xFFD200FF, 
    /* 4288  */ 0xFFD100FF, 
    /* 4289  */ 0xFFCF00FF, 
    /* 4290  */ 0xFFCE00FF, 
    /* 4291  */ 0xFFCD00FF, 
    /* 4292  */ 0xFFCB00FF, 
    /* 4293  */ 0xFFC900FF, 
    /* 4294  */ 0xFFC700FF, 
    /* 4295  */ 0xFFC400FF, 
    /* 4296  */ 0xFFC200FF, 
    /* 4297  */ 0xFFC000FF, 
    /* 4298  */ 0xFFBE00FF, 
    /* 4299  */ 0xFFBC00FF, 
    /* 4300  */ 0xFFBA00FF, 
    /* 4301  */ 0xFFB800FF, 
    /* 4302  */ 0xFFB500FF, 
    /* 4303  */ 0xFFB300FF, 
    /* 4304  */ 0xFFB100FF, 
    /* 4305  */ 0xFFAF00FF, 
    /* 4306  */ 0xFFAD00FF, 
    /* 4307  */ 0xFFAB00FF, 
    /* 4308  */ 0xFFA900FF, 
    /* 4309  */ 0xFFA600FF, 
    /* 4310  */ 0xFFA400FF, 
    /* 4311  */ 0xFFA200FF, 
    /* 4312  */ 0xFFA000FF, 
    /* 4313  */ 0xFF9E00FF, 
    /* 4314  */ 0xFF9C00FF, 
    /* 4315  */ 0xFF9A00FF, 
    /* 4316  */ 0xFF9700FF, 
    /* 4317  */ 0xFF9500FF, 
    /* 4318  */ 0xFF9300FF, 
    /* 4319  */ 0xFF9100FF, 
    /* 4320  */ 0xFF8F00FF, 
    /* 4321  */ 0xFF8D00FF, 
    /* 4322  */ 0xFF8B00FF, 
    /* 4323  */ 0xFF8800FF, 
    /* 4324  */ 0xFF8600FF, 
    /* 4325  */ 0xFF8400FF, 
    /* 4326  */ 0xFF8200FF, 
    /* 4327  */ 0xFF8000FF, 
    /* 4328  */ 0xFF7D00FF, 
    /* 4329  */ 0xFF7900FF, 
    /* 4330  */ 0xFF7600FF, 
    /* 4331  */ 0xFF7200FF, 
    /* 4332  */ 0xFF6F00FF, 
    /* 4333  */ 0xFF6B00FF, 
    /* 4334  */ 0xFF6800FF, 
    /* 4335  */ 0xFF6400FF, 
    /* 4336  */ 0xFF6000FF, 
    /* 4337  */ 0xFF5D00FF, 
    /* 4338  */ 0xFF5900FF, 
    /* 4339  */ 0xFF5600FF, 
    /* 4340  */ 0xFF5200FF, 
    /* 4341  */ 0xFF4F00FF, 
    /* 4342  */ 0xFF4B00FF, 
    /* 4343  */ 0xFF4700FF, 
    /* 4344  */ 0xFF4400FF, 
    /* 4345  */ 0xFF4000FF, 
    /* 4346  */ 0xFF3D00FF, 
    /* 4347  */ 0xFF3900FF, 
    /* 4348  */ 0xFF3600FF, 
    /* 4349  */ 0xFF3200FF, 
    /* 4350  */ 0xFF2E00FF, 
    /* 4351  */ 0xFF2B00FF, 
    /* 4352  */ 0xFF2700FF, 
    /* 4353  */ 0xFF2400FF, 
    /* 4354  */ 0xFF2000FF, 
    /* 4355  */ 0xFF1D00FF, 
    /* 4356  */ 0xFF1900FF, 
    /* 4357  */ 0xFF1500FF, 
    /* 4358  */ 0xFF1200FF, 
    /* 4359  */ 0xFF0E00FF, 
    /* 4360  */ 0xFF0B00FF, 
    /* 4361  */ 0xFF0700FF, 
    /* 4362  */ 0xFF0400FF, 
    /* 4363  */ 0xFF0000FF, 
    /* 4364  */ 0xFF0000FF, 
    /* 4365  */ 0xFF0000FF, 
    /* 4366  */ 0xFF0000FF, 
    /* 4367  */ 0xFF0000FF, 
    /* 4368  */ 0xFF0000FF, 
    /* 4369  */ 0xFF0000FF, 
    /* 4370  */ 0xFF0000FF, 
    /* 4371  */ 0xFF0000FF, 
    /* 4372  */ 0xFF0000FF, 
    /* 4373  */ 0xFF0000FF, 
    /* 4374  */ 0xFF0000FF, 
    /* 4375  */ 0xFF0000FF, 
    /* 4376  */ 0xFF0000FF, 
    /* 4377  */ 0xFF0000FF, 
    /* 4378  */ 0xFF0000FF, 
    /* 4379  */ 0xFF0000FF, 
    /* 4380  */ 0xFF0000FF, 
    /* 4381  */ 0xFF0000FF, 
    /* 4382  */ 0xFF0000FF, 
    /* 4383  */ 0xFF0000FF, 
    /* 4384  */ 0xFF0000FF, 
    /* 4385  */ 0xFF0000FF, 
    /* 4386  */ 0xFF0000FF, 
    /* 4387  */ 0xFF0000FF, 
    /* 4388  */ 0xFF0000FF, 
    /* 4389  */ 0xFF0000FF, 
    /* 4390  */ 0xFF0000FF, 
    /* 4391  */ 0xFF0000FF, 
    /* 4392  */ 0xFF0000FF, 
    /* 4393  */ 0xFF0000FF, 
    /* 4394  */ 0xFF0000FF, 
    /* 4395  */ 0xFF0000FF, 
    /* 4396  */ 0xFF0000FF, 
    /* 4397  */ 0xFF0000FF, 
    /* 4398  */ 0xFF0000FF, 
    /* 4399  */ 0xFF0000FF, 
    /* 4400  */ 0xFF0001FF, 
    /* 4401  */ 0xFF0003FF, 
    /* 4402  */ 0xFF0004FF, 
    /* 4403  */ 0xFF0005FF, 
    /* 4404  */ 0xFF0007FF, 
    /* 4405  */ 0xFF0008FF, 
    /* 4406  */ 0xFF000AFF, 
    /* 4407  */ 0xFF000BFF, 
    /* 4408  */ 0xFF000DFF, 
    /* 4409  */ 0xFF000EFF, 
    /* 4410  */ 0xFF000FFF, 
    /* 4411  */ 0xFF0011FF, 
    /* 4412  */ 0xFF0012FF, 
    /* 4413  */ 0xFF0014FF, 
    /* 4414  */ 0xFF0015FF, 
    /* 4415  */ 0xFF0017FF, 
    /* 4416  */ 0xFF0018FF, 
    /* 4417  */ 0xFF0019FF, 
    /* 4418  */ 0xFF001BFF, 
    /* 4419  */ 0xFF001CFF, 
    /* 4420  */ 0xFF001EFF, 
    /* 4421  */ 0xFF001FFF, 
    /* 4422  */ 0xFF0021FF, 
    /* 4423  */ 0xFF0022FF, 
    /* 4424  */ 0xFF0023FF, 
    /* 4425  */ 0xFF0025FF, 
    /* 4426  */ 0xFF0026FF, 
    /* 4427  */ 0xFF0028FF, 
    /* 4428  */ 0xFF0029FF, 
    /* 4429  */ 0xFF002BFF, 
    /* 4430  */ 0xFF002CFF, 
    /* 4431  */ 0xFF002DFF, 
    /* 4432  */ 0xFF002FFF, 
    /* 4433  */ 0xFF0030FF, 
    /* 4434  */ 0xFF0032FF, 
    /* 4435  */ 0xFF0033FF, 
    /* 4436  */ 0xFF0036FF, 
    /* 4437  */ 0xFF0038FF, 
    /* 4438  */ 0xFF003AFF, 
    /* 4439  */ 0xFF003CFF, 
    /* 4440  */ 0xFF003EFF, 
    /* 4441  */ 0xFF0040FF, 
    /* 4442  */ 0xFF0042FF, 
    /* 4443  */ 0xFF0045FF, 
    /* 4444  */ 0xFF0047FF, 
    /* 4445  */ 0xFF0049FF, 
    /* 4446  */ 0xFF004BFF, 
    /* 4447  */ 0xFF004DFF, 
    /* 4448  */ 0xFF004FFF, 
    /* 4449  */ 0xFF0051FF, 
    /* 4450  */ 0xFF0054FF, 
    /* 4451  */ 0xFF0056FF, 
    /* 4452  */ 0xFF0058FF, 
    /* 4453  */ 0xFF005AFF, 
    /* 4454  */ 0xFF005CFF, 
    /* 4455  */ 0xFF005EFF, 
    /* 4456  */ 0xFF0060FF, 
    /* 4457  */ 0xFF0063FF, 
    /* 4458  */ 0xFF0065FF, 
    /* 4459  */ 0xFF0067FF, 
    /* 4460  */ 0xFF0069FF, 
    /* 4461  */ 0xFF006BFF, 
    /* 4462  */ 0xFF006DFF, 
    /* 4463  */ 0xFF006FFF, 
    /* 4464  */ 0xFF0072FF, 
    /* 4465  */ 0xFF0074FF, 
    /* 4466  */ 0xFF0076FF, 
    /* 4467  */ 0xFF0078FF, 
    /* 4468  */ 0xFF007AFF, 
    /* 4469  */ 0xFF007CFF, 
    /* 4470  */ 0xFF007EFF, 
    /* 4471  */ 0xFF0081FF, 
    /* 4472  */ 0xFF0083FF, 
    /* 4473  */ 0xFF0085FF, 
    /* 4474  */ 0xFF0087FF, 
    /* 4475  */ 0xFF0089FF, 
    /* 4476  */ 0xFF008BFF, 
    /* 4477  */ 0xFF008DFF, 
    /* 4478  */ 0xFF0090FF, 
    /* 4479  */ 0xFF0092FF, 
    /* 4480  */ 0xFF0094FF, 
    /* 4481  */ 0xFF0096FF, 
    /* 4482  */ 0xFF0098FF, 
    /* 4483  */ 0xFF009AFF, 
    /* 4484  */ 0xFF009CFF, 
    /* 4485  */ 0xFF009FFF, 
    /* 4486  */ 0xFF00A1FF, 
    /* 4487  */ 0xFF00A3FF, 
    /* 4488  */ 0xFF00A5FF, 
    /* 4489  */ 0xFF00A7FF, 
    /* 4490  */ 0xFF00A9FF, 
    /* 4491  */ 0xFF00ABFF, 
    /* 4492  */ 0xFF00AEFF, 
    /* 4493  */ 0xFF00B0FF, 
    /* 4494  */ 0xFF00B2FF, 
    /* 4495  */ 0xFF00B4FF, 
    /* 4496  */ 0xFF00B6FF, 
    /* 4497  */ 0xFF00B8FF, 
    /* 4498  */ 0xFF00BAFF, 
    /* 4499  */ 0xFF00BDFF, 
    /* 4500  */ 0xFF00BFFF, 
    /* 4501  */ 0xFF00C1FF, 
    /* 4502  */ 0xFF00C3FF, 
    /* 4503  */ 0xFF00C5FF, 
    /* 4504  */ 0xFF00C7FF, 
    /* 4505  */ 0xFF00C9FF, 
    /* 4506  */ 0xFF00CBFF, 
    /* 4507  */ 0xFF00CDFF, 
    /* 4508  */ 0xFF00CEFF, 
    /* 4509  */ 0xFF00D0FF, 
    /* 4510  */ 0xFF00D1FF, 
    /* 4511  */ 0xFF00D3FF, 
    /* 4512  */ 0xFF00D4FF, 
    /* 4513  */ 0xFF00D5FF, 
    /* 4514  */ 0xFF00D7FF, 
    /* 4515  */ 0xFF00D8FF, 
    /* 4516  */ 0xFF00DAFF, 
    /* 4517  */ 0xFF00DBFF, 
    /* 4518  */ 0xFF00DDFF, 
    /* 4519  */ 0xFF00DEFF, 
    /* 4520  */ 0xFF00DFFF, 
    /* 4521  */ 0xFF00E1FF, 
    /* 4522  */ 0xFF00E2FF, 
    /* 4523  */ 0xFF00E4FF, 
    /* 4524  */ 0xFF00E5FF, 
    /* 4525  */ 0xFF00E7FF, 
    /* 4526  */ 0xFF00E8FF, 
    /* 4527  */ 0xFF00E9FF, 
    /* 4528  */ 0xFF00EBFF, 
    /* 4529  */ 0xFF00ECFF, 
    /* 4530  */ 0xFF00EEFF, 
    /* 4531  */ 0xFF00EFFF, 
    /* 4532  */ 0xFF00F1FF, 
    /* 4533  */ 0xFF00F2FF, 
    /* 4534  */ 0xFF00F3FF, 
    /* 4535  */ 0xFF00F5FF, 
    /* 4536  */ 0xFF00F6FF, 
    /* 4537  */ 0xFF00F8FF, 
    /* 4538  */ 0xFF00F9FF, 
    /* 4539  */ 0xFF00FBFF, 
    /* 4540  */ 0xFF00FCFF, 
    /* 4541  */ 0xFF00FDFF, 
    /* 4542  */ 0xFF00FFFE, 
    /* 4543  */ 0xFF00FFFD, 
    /* 4544  */ 0xFF00FFFB, 
    /* 4545  */ 0xFF00FFFA, 
    /* 4546  */ 0xFF00FFF8, 
    /* 4547  */ 0xFF00FFF7, 
    /* 4548  */ 0xFF00FFF6, 
    /* 4549  */ 0xFF00FFF4, 
    /* 4550  */ 0xFF00FFF3, 
    /* 4551  */ 0xFF00FFF1, 
    /* 4552  */ 0xFF00FFF0, 
    /* 4553  */ 0xFF00FFEE, 
    /* 4554  */ 0xFF00FFED, 
    /* 4555  */ 0xFF00FFEC, 
    /* 4556  */ 0xFF00FFEA, 
    /* 4557  */ 0xFF00FFE9, 
    /* 4558  */ 0xFF00FFE7, 
    /* 4559  */ 0xFF00FFE6, 
    /* 4560  */ 0xFF00FFE4, 
    /* 4561  */ 0xFF00FFE3, 
    /* 4562  */ 0xFF00FFE2, 
    /* 4563  */ 0xFF00FFE0, 
    /* 4564  */ 0xFF00FFDF, 
    /* 4565  */ 0xFF00FFDD, 
    /* 4566  */ 0xFF00FFDC, 
    /* 4567  */ 0xFF00FFDA, 
    /* 4568  */ 0xFF00FFD9, 
    /* 4569  */ 0xFF00FFD8, 
    /* 4570  */ 0xFF00FFD6, 
    /* 4571  */ 0xFF00FFD5, 
    /* 4572  */ 0xFF00FFD3, 
    /* 4573  */ 0xFF00FFD2, 
    /* 4574  */ 0xFF00FFD0, 
    /* 4575  */ 0xFF00FFCF, 
    /* 4576  */ 0xFF00FFCE, 
    /* 4577  */ 0xFF00FFCC, 
    /* 4578  */ 0xFF00FFCA, 
    /* 4579  */ 0xFF00FFC8, 
    /* 4580  */ 0xFF00FFC6, 
    /* 4581  */ 0xFF00FFC4, 
    /* 4582  */ 0xFF00FFC2, 
    /* 4583  */ 0xFF00FFC0, 
    /* 4584  */ 0xFF00FFBD, 
    /* 4585  */ 0xFF00FFBB, 
    /* 4586  */ 0xFF00FFB9, 
    /* 4587  */ 0xFF00FFB7, 
    /* 4588  */ 0xFF00FFB5, 
    /* 4589  */ 0xFF00FFB3, 
    /* 4590  */ 0xFF00FFB1, 
    /* 4591  */ 0xFF00FFAE, 
    /* 4592  */ 0xFF00FFAC, 
    /* 4593  */ 0xFF00FFAA, 
    /* 4594  */ 0xFF00FFA8, 
    /* 4595  */ 0xFF00FFA6, 
    /* 4596  */ 0xFF00FFA4, 
    /* 4597  */ 0xFF00FFA2, 
    /* 4598  */ 0xFF00FF9F, 
    /* 4599  */ 0xFF00FF9D, 
    /* 4600  */ 0xFF00FF9B, 
    /* 4601  */ 0xFF00FF99, 
    /* 4602  */ 0xFF00FF97, 
    /* 4603  */ 0xFF00FF95, 
    /* 4604  */ 0xFF00FF93, 
    /* 4605  */ 0xFF00FF90, 
    /* 4606  */ 0xFF00FF8E, 
    /* 4607  */ 0xFF00FF8C, 
    /* 4608  */ 0xFF00FF8A, 
    /* 4609  */ 0xFF00FF88, 
    /* 4610  */ 0xFF00FF86, 
    /* 4611  */ 0xFF00FF84, 
    /* 4612  */ 0xFF00FF81, 
    /* 4613  */ 0xFF00FF7F, 
    /* 4614  */ 0xFF00FF7D, 
    /* 4615  */ 0xFF00FF7B, 
    /* 4616  */ 0xFF00FF79, 
    /* 4617  */ 0xFF00FF77, 
    /* 4618  */ 0xFF00FF75, 
    /* 4619  */ 0xFF00FF72, 
    /* 4620  */ 0xFF00FF70, 
    /* 4621  */ 0xFF00FF6E, 
    /* 4622  */ 0xFF00FF6C, 
    /* 4623  */ 0xFF00FF6A, 
    /* 4624  */ 0xFF00FF68, 
    /* 4625  */ 0xFF00FF66, 
    /* 4626  */ 0xFF00FF63, 
    /* 4627  */ 0xFF00FF61, 
    /* 4628  */ 0xFF00FF5F, 
    /* 4629  */ 0xFF00FF5D, 
    /* 4630  */ 0xFF00FF5B, 
    /* 4631  */ 0xFF00FF59, 
    /* 4632  */ 0xFF00FF57, 
    /* 4633  */ 0xFF00FF54, 
    /* 4634  */ 0xFF00FF52, 
    /* 4635  */ 0xFF00FF50, 
    /* 4636  */ 0xFF00FF4E, 
    /* 4637  */ 0xFF00FF4C, 
    /* 4638  */ 0xFF00FF4A, 
    /* 4639  */ 0xFF00FF48, 
    /* 4640  */ 0xFF00FF45, 
    /* 4641  */ 0xFF00FF43, 
    /* 4642  */ 0xFF00FF41, 
    /* 4643  */ 0xFF00FF3F, 
    /* 4644  */ 0xFF00FF3D, 
    /* 4645  */ 0xFF00FF3B, 
    /* 4646  */ 0xFF00FF39, 
    /* 4647  */ 0xFF00FF36, 
    /* 4648  */ 0xFF00FF34, 
    /* 4649  */ 0xFF00FF32, 
    /* 4650  */ 0xFF00FF31, 
    /* 4651  */ 0xFF00FF2F, 
    /* 4652  */ 0xFF00FF2E, 
    /* 4653  */ 0xFF00FF2D, 
    /* 4654  */ 0xFF00FF2B, 
    /* 4655  */ 0xFF00FF2A, 
    /* 4656  */ 0xFF00FF28, 
    /* 4657  */ 0xFF00FF27, 
    /* 4658  */ 0xFF00FF25, 
    /* 4659  */ 0xFF00FF24, 
    /* 4660  */ 0xFF00FF23, 
    /* 4661  */ 0xFF00FF21, 
    /* 4662  */ 0xFF00FF20, 
    /* 4663  */ 0xFF00FF1E, 
    /* 4664  */ 0xFF00FF1D, 
    /* 4665  */ 0xFF00FF1B, 
    /* 4666  */ 0xFF00FF1A, 
    /* 4667  */ 0xFF00FF19, 
    /* 4668  */ 0xFF00FF17, 
    /* 4669  */ 0xFF00FF16, 
    /* 4670  */ 0xFF00FF14, 
    /* 4671  */ 0xFF00FF13, 
    /* 4672  */ 0xFF00FF11, 
    /* 4673  */ 0xFF00FF10, 
    /* 4674  */ 0xFF00FF0F, 
    /* 4675  */ 0xFF00FF0D, 
    /* 4676  */ 0xFF00FF0C, 
    /* 4677  */ 0xFF00FF0A, 
    /* 4678  */ 0xFF00FF09, 
    /* 4679  */ 0xFF00FF07, 
    /* 4680  */ 0xFF00FF06, 
    /* 4681  */ 0xFF00FF05, 
    /* 4682  */ 0xFF00FF03, 
    /* 4683  */ 0xFF00FF02, 
    /* 4684  */ 0xFF00FF00, 
    /* 4685  */ 0xFF00FF00, 
    /* 4686  */ 0xFF02FF00, 
    /* 4687  */ 0xFF03FF00, 
    /* 4688  */ 0xFF04FF00, 
    /* 4689  */ 0xFF06FF00, 
    /* 4690  */ 0xFF07FF00, 
    /* 4691  */ 0xFF09FF00, 
    /* 4692  */ 0xFF0AFF00, 
    /* 4693  */ 0xFF0CFF00, 
    /* 4694  */ 0xFF0DFF00, 
    /* 4695  */ 0xFF0EFF00, 
    /* 4696  */ 0xFF10FF00, 
    /* 4697  */ 0xFF11FF00, 
    /* 4698  */ 0xFF13FF00, 
    /* 4699  */ 0xFF14FF00, 
    /* 4700  */ 0xFF16FF00, 
    /* 4701  */ 0xFF17FF00, 
    /* 4702  */ 0xFF18FF00, 
    /* 4703  */ 0xFF1AFF00, 
    /* 4704  */ 0xFF1BFF00, 
    /* 4705  */ 0xFF1DFF00, 
    /* 4706  */ 0xFF1EFF00, 
    /* 4707  */ 0xFF20FF00, 
    /* 4708  */ 0xFF21FF00, 
    /* 4709  */ 0xFF22FF00, 
    /* 4710  */ 0xFF24FF00, 
    /* 4711  */ 0xFF25FF00, 
    /* 4712  */ 0xFF27FF00, 
    /* 4713  */ 0xFF28FF00, 
    /* 4714  */ 0xFF2AFF00, 
    /* 4715  */ 0xFF2BFF00, 
    /* 4716  */ 0xFF2CFF00, 
    /* 4717  */ 0xFF2EFF00, 
    /* 4718  */ 0xFF2FFF00, 
    /* 4719  */ 0xFF31FF00, 
    /* 4720  */ 0xFF32FF00, 
    /* 4721  */ 0xFF34FF00, 
    /* 4722  */ 0xFF36FF00, 
    /* 4723  */ 0xFF38FF00, 
    /* 4724  */ 0xFF3AFF00, 
    /* 4725  */ 0xFF3DFF00, 
    /* 4726  */ 0xFF3FFF00, 
    /* 4727  */ 0xFF41FF00, 
    /* 4728  */ 0xFF43FF00, 
    /* 4729  */ 0xFF45FF00, 
    /* 4730  */ 0xFF47FF00, 
    /* 4731  */ 0xFF49FF00, 
    /* 4732  */ 0xFF4CFF00, 
    /* 4733  */ 0xFF4EFF00, 
    /* 4734  */ 0xFF50FF00, 
    /* 4735  */ 0xFF52FF00, 
    /* 4736  */ 0xFF54FF00, 
    /* 4737  */ 0xFF56FF00, 
    /* 4738  */ 0xFF58FF00, 
    /* 4739  */ 0xFF5BFF00, 
    /* 4740  */ 0xFF5DFF00, 
    /* 4741  */ 0xFF5FFF00, 
    /* 4742  */ 0xFF61FF00, 
    /* 4743  */ 0xFF63FF00, 
    /* 4744  */ 0xFF65FF00, 
    /* 4745  */ 0xFF67FF00, 
    /* 4746  */ 0xFF6AFF00, 
    /* 4747  */ 0xFF6CFF00, 
    /* 4748  */ 0xFF6EFF00, 
    /* 4749  */ 0xFF70FF00, 
    /* 4750  */ 0xFF72FF00, 
    /* 4751  */ 0xFF74FF00, 
    /* 4752  */ 0xFF76FF00, 
    /* 4753  */ 0xFF79FF00, 
    /* 4754  */ 0xFF7BFF00, 
    /* 4755  */ 0xFF7DFF00, 
    /* 4756  */ 0xFF7FFF00, 
    /* 4757  */ 0xFF81FF00, 
    /* 4758  */ 0xFF83FF00, 
    /* 4759  */ 0xFF85FF00, 
    /* 4760  */ 0xFF88FF00, 
    /* 4761  */ 0xFF8AFF00, 
    /* 4762  */ 0xFF8CFF00, 
    /* 4763  */ 0xFF8EFF00, 
    /* 4764  */ 0xFF90FF00, 
    /* 4765  */ 0xFF92FF00, 
    /* 4766  */ 0xFF94FF00, 
    /* 4767  */ 0xFF97FF00, 
    /* 4768  */ 0xFF99FF00, 
    /* 4769  */ 0xFF9BFF00, 
    /* 4770  */ 0xFF9DFF00, 
    /* 4771  */ 0xFF9FFF00, 
    /* 4772  */ 0xFFA1FF00, 
    /* 4773  */ 0xFFA3FF00, 
    /* 4774  */ 0xFFA6FF00, 
    /* 4775  */ 0xFFA8FF00, 
    /* 4776  */ 0xFFAAFF00, 
    /* 4777  */ 0xFFACFF00, 
    /* 4778  */ 0xFFAEFF00, 
    /* 4779  */ 0xFFB0FF00, 
    /* 4780  */ 0xFFB2FF00, 
    /* 4781  */ 0xFFB5FF00, 
    /* 4782  */ 0xFFB7FF00, 
    /* 4783  */ 0xFFB9FF00, 
    /* 4784  */ 0xFFBBFF00, 
    /* 4785  */ 0xFFBDFF00, 
    /* 4786  */ 0xFFBFFF00, 
    /* 4787  */ 0xFFC1FF00, 
    /* 4788  */ 0xFFC4FF00, 
    /* 4789  */ 0xFFC6FF00, 
    /* 4790  */ 0xFFC8FF00, 
    /* 4791  */ 0xFFCAFF00, 
    /* 4792  */ 0xFFCCFF00, 
    /* 4793  */ 0xFFCDFF00, 
    /* 4794  */ 0xFFCFFF00, 
    /* 4795  */ 0xFFD0FF00, 
    /* 4796  */ 0xFFD2FF00, 
    /* 4797  */ 0xFFD3FF00, 
    /* 4798  */ 0xFFD4FF00, 
    /* 4799  */ 0xFFD6FF00, 
    /* 4800  */ 0xFFD7FF00, 
    /* 4801  */ 0xFFD9FF00, 
    /* 4802  */ 0xFFDAFF00, 
    /* 4803  */ 0xFFDCFF00, 
    /* 4804  */ 0xFFDDFF00, 
    /* 4805  */ 0xFFDEFF00, 
    /* 4806  */ 0xFFE0FF00, 
    /* 4807  */ 0xFFE1FF00, 
    /* 4808  */ 0xFFE3FF00, 
    /* 4809  */ 0xFFE4FF00, 
    /* 4810  */ 0xFFE6FF00, 
    /* 4811  */ 0xFFE7FF00, 
    /* 4812  */ 0xFFE8FF00, 
    /* 4813  */ 0xFFEAFF00, 
    /* 4814  */ 0xFFEBFF00, 
    /* 4815  */ 0xFFEDFF00, 
    /* 4816  */ 0xFFEEFF00, 
    /* 4817  */ 0xFFF0FF00, 
    /* 4818  */ 0xFFF1FF00, 
    /* 4819  */ 0xFFF2FF00, 
    /* 4820  */ 0xFFF4FF00, 
    /* 4821  */ 0xFFF5FF00, 
    /* 4822  */ 0xFFF7FF00, 
    /* 4823  */ 0xFFF8FF00, 
    /* 4824  */ 0xFFFAFF00, 
    /* 4825  */ 0xFFFBFF00, 
    /* 4826  */ 0xFFFCFF00, 
    /* 4827  */ 0xFFFEFF00, 
    /* 4828  */ 0xFFFFFE00, 
    /* 4829  */ 0xFFFFFD00, 
    /* 4830  */ 0xFFFFFD00, 
    /* 4831  */ 0xFFFFFC00, 
    /* 4832  */ 0xFFFFFB00, 
    /* 4833  */ 0xFFFFFB00, 
    /* 4834  */ 0xFFFFFA00, 
    /* 4835  */ 0xFFFFF900, 
    /* 4836  */ 0xFFFFF800, 
    /* 4837  */ 0xFFFFF800, 
    /* 4838  */ 0xFFFFF700, 
    /* 4839  */ 0xFFFFF600, 
    /* 4840  */ 0xFFFFF600, 
    /* 4841  */ 0xFFFFF500, 
    /* 4842  */ 0xFFFFF400, 
    /* 4843  */ 0xFFFFF300, 
    /* 4844  */ 0xFFFFF300, 
    /* 4845  */ 0xFFFFF200, 
    /* 4846  */ 0xFFFFF100, 
    /* 4847  */ 0xFFFFF100, 
    /* 4848  */ 0xFFFFF000, 
    /* 4849  */ 0xFFFFEF00, 
    /* 4850  */ 0xFFFFEE00, 
    /* 4851  */ 0xFFFFEE00, 
    /* 4852  */ 0xFFFFED00, 
    /* 4853  */ 0xFFFFEC00, 
    /* 4854  */ 0xFFFFEC00, 
    /* 4855  */ 0xFFFFEB00, 
    /* 4856  */ 0xFFFFEA00, 
    /* 4857  */ 0xFFFFE900, 
    /* 4858  */ 0xFFFFE900, 
    /* 4859  */ 0xFFFFE800, 
    /* 4860  */ 0xFFFFE700, 
    /* 4861  */ 0xFFFFE700, 
    /* 4862  */ 0xFFFFE600, 
    /* 4863  */ 0xFFFFE500, 
    /* 4864  */ 0xFFFFE400, 
    /* 4865  */ 0xFFFFE300, 
    /* 4866  */ 0xFFFFE200, 
    /* 4867  */ 0xFFFFE100, 
    /* 4868  */ 0xFFFFE000, 
    /* 4869  */ 0xFFFFDF00, 
    /* 4870  */ 0xFFFFDE00, 
    /* 4871  */ 0xFFFFDD00, 
    /* 4872  */ 0xFFFFDC00, 
    /* 4873  */ 0xFFFFDA00, 
    /* 4874  */ 0xFFFFD900, 
    /* 4875  */ 0xFFFFD800, 
    /* 4876  */ 0xFFFFD700, 
    /* 4877  */ 0xFFFFD600, 
    /* 4878  */ 0xFFFFD500, 
    /* 4879  */ 0xFFFFD400, 
    /* 4880  */ 0xFFFFD300, 
    /* 4881  */ 0xFFFFD200, 
    /* 4882  */ 0xFFFFD100, 
    /* 4883  */ 0xFFFFD000, 
    /* 4884  */ 0xFFFFCF00, 
    /* 4885  */ 0xFFFFCE00, 
    /* 4886  */ 0xFFFFCD00, 
    /* 4887  */ 0xFFFFCB00, 
    /* 4888  */ 0xFFFFCA00, 
    /* 4889  */ 0xFFFFC900, 
    /* 4890  */ 0xFFFFC800, 
    /* 4891  */ 0xFFFFC700, 
    /* 4892  */ 0xFFFFC600, 
    /* 4893  */ 0xFFFFC500, 
    /* 4894  */ 0xFFFFC400, 
    /* 4895  */ 0xFFFFC300, 
    /* 4896  */ 0xFFFFC200, 
    /* 4897  */ 0xFFFFC100, 
    /* 4898  */ 0xFFFFC000, 
    /* 4899  */ 0xFFFFBF00, 
    /* 4900  */ 0xFFFFBE00, 
    /* 4901  */ 0xFFFFBC00, 
    /* 4902  */ 0xFFFFBB00, 
    /* 4903  */ 0xFFFFBA00, 
    /* 4904  */ 0xFFFFB900, 
    /* 4905  */ 0xFFFFB800, 
    /* 4906  */ 0xFFFFB700, 
    /* 4907  */ 0xFFFFB600, 
    /* 4908  */ 0xFFFFB500, 
    /* 4909  */ 0xFFFFB400, 
    /* 4910  */ 0xFFFFB300, 
    /* 4911  */ 0xFFFFB200, 
    /* 4912  */ 0xFFFFB100, 
    /* 4913  */ 0xFFFFB000, 
    /* 4914  */ 0xFFFFAF00, 
    /* 4915  */ 0xFFFFAD00, 
    /* 4916  */ 0xFFFFAC00, 
    /* 4917  */ 0xFFFFAB00, 
    /* 4918  */ 0xFFFFAA00, 
    /* 4919  */ 0xFFFFA900, 
    /* 4920  */ 0xFFFFA800, 
    /* 4921  */ 0xFFFFA700, 
    /* 4922  */ 0xFFFFA600, 
    /* 4923  */ 0xFFFFA500, 
    /* 4924  */ 0xFFFFA400, 
    /* 4925  */ 0xFFFFA300, 
    /* 4926  */ 0xFFFFA200, 
    /* 4927  */ 0xFFFFA100, 
    /* 4928  */ 0xFFFFA000, 
    /* 4929  */ 0xFFFF9E00, 
    /* 4930  */ 0xFFFF9D00, 
    /* 4931  */ 0xFFFF9C00, 
    /* 4932  */ 0xFFFF9B00, 
    /* 4933  */ 0xFFFF9A00, 
    /* 4934  */ 0xFFFF9900, 
    /* 4935  */ 0xFFFF9800, 
    /* 4936  */ 0xFFFF9700, 
    /* 4937  */ 0xFFFF9700, 
    /* 4938  */ 0xFFFF9600, 
    /* 4939  */ 0xFFFF9500, 
    /* 4940  */ 0xFFFF9500, 
    /* 4941  */ 0xFFFF9400, 
    /* 4942  */ 0xFFFF9300, 
    /* 4943  */ 0xFFFF9200, 
    /* 4944  */ 0xFFFF9200, 
    /* 4945  */ 0xFFFF9100, 
    /* 4946  */ 0xFFFF9000, 
    /* 4947  */ 0xFFFF9000, 
    /* 4948  */ 0xFFFF8F00, 
    /* 4949  */ 0xFFFF8E00, 
    /* 4950  */ 0xFFFF8D00, 
    /* 4951  */ 0xFFFF8D00, 
    /* 4952  */ 0xFFFF8C00, 
    /* 4953  */ 0xFFFF8B00, 
    /* 4954  */ 0xFFFF8B00, 
    /* 4955  */ 0xFFFF8A00, 
    /* 4956  */ 0xFFFF8900, 
    /* 4957  */ 0xFFFF8800, 
    /* 4958  */ 0xFFFF8800, 
    /* 4959  */ 0xFFFF8700, 
    /* 4960  */ 0xFFFF8600, 
    /* 4961  */ 0xFFFF8600, 
    /* 4962  */ 0xFFFF8500, 
    /* 4963  */ 0xFFFF8400, 
    /* 4964  */ 0xFFFF8300, 
    /* 4965  */ 0xFFFF8300, 
    /* 4966  */ 0xFFFF8200, 
    /* 4967  */ 0xFFFF8100, 
    /* 4968  */ 0xFFFF8100, 
    /* 4969  */ 0xFFFF8000, 
    /* 4970  */ 0xFFFF7F00, 
    /* 4971  */ 0xFFFF7E00, 
    /* 4972  */ 0xFFFF7E00, 
    /* 4973  */ 0xFFFF7D00, 
    /* 4974  */ 0xFFFF7C00, 
    /* 4975  */ 0xFFFF7C00, 
    /* 4976  */ 0xFFFF7B00, 
    /* 4977  */ 0xFFFF7A00, 
    /* 4978  */ 0xFFFF7900, 
    /* 4979  */ 0xFFFF7900, 
    /* 4980  */ 0xFFFF7800, 
    /* 4981  */ 0xFFFF7700, 
    /* 4982  */ 0xFFFF7700, 
    /* 4983  */ 0xFFFF7600, 
    /* 4984  */ 0xFFFF7500, 
    /* 4985  */ 0xFFFF7400, 
    /* 4986  */ 0xFFFF7400, 
    /* 4987  */ 0xFFFF7300, 
    /* 4988  */ 0xFFFF7200, 
    /* 4989  */ 0xFFFF7200, 
    /* 4990  */ 0xFFFF7100, 
    /* 4991  */ 0xFFFF7000, 
    /* 4992  */ 0xFFFF6F00, 
    /* 4993  */ 0xFFFF6F00, 
    /* 4994  */ 0xFFFF6E00, 
    /* 4995  */ 0xFFFF6D00, 
    /* 4996  */ 0xFFFF6D00, 
    /* 4997  */ 0xFFFF6C00, 
    /* 4998  */ 0xFFFF6B00, 
    /* 4999  */ 0xFFFF6A00, 
    /* 5000  */ 0xFFFF6A00, 
    /* 5001  */ 0xFFFF6900, 
    /* 5002  */ 0xFFFF6800, 
    /* 5003  */ 0xFFFF6800, 
    /* 5004  */ 0xFFFF6700, 
    /* 5005  */ 0xFFFF6600, 
    /* 5006  */ 0xFFFF6600, 
    /* 5007  */ 0xFFFF6500, 
    /* 5008  */ 0xFFFF6400, 
    /* 5009  */ 0xFFFF6300, 
    /* 5010  */ 0xFFFF6300, 
    /* 5011  */ 0xFFFF6200, 
    /* 5012  */ 0xFFFF6100, 
    /* 5013  */ 0xFFFF6100, 
    /* 5014  */ 0xFFFF6000, 
    /* 5015  */ 0xFFFF5F00, 
    /* 5016  */ 0xFFFF5E00, 
    /* 5017  */ 0xFFFF5E00, 
    /* 5018  */ 0xFFFF5D00, 
    /* 5019  */ 0xFFFF5C00, 
    /* 5020  */ 0xFFFF5C00, 
    /* 5021  */ 0xFFFF5B00, 
    /* 5022  */ 0xFFFF5A00, 
    /* 5023  */ 0xFFFF5900, 
    /* 5024  */ 0xFFFF5900, 
    /* 5025  */ 0xFFFF5800, 
    /* 5026  */ 0xFFFF5700, 
    /* 5027  */ 0xFFFF5700, 
    /* 5028  */ 0xFFFF5600, 
    /* 5029  */ 0xFFFF5500, 
    /* 5030  */ 0xFFFF5400, 
    /* 5031  */ 0xFFFF5400, 
    /* 5032  */ 0xFFFF5300, 
    /* 5033  */ 0xFFFF5200, 
    /* 5034  */ 0xFFFF5200, 
    /* 5035  */ 0xFFFF5100, 
    /* 5036  */ 0xFFFF5000, 
    /* 5037  */ 0xFFFF4F00, 
    /* 5038  */ 0xFFFF4F00, 
    /* 5039  */ 0xFFFF4E00, 
    /* 5040  */ 0xFFFF4D00, 
    /* 5041  */ 0xFFFF4D00, 
    /* 5042  */ 0xFFFF4C00, 
    /* 5043  */ 0xFFFF4B00, 
    /* 5044  */ 0xFFFF4A00, 
    /* 5045  */ 0xFFFF4A00, 
    /* 5046  */ 0xFFFF4900, 
    /* 5047  */ 0xFFFF4800, 
    /* 5048  */ 0xFFFF4800, 
    /* 5049  */ 0xFFFF4700, 
    /* 5050  */ 0xFFFF4600, 
    /* 5051  */ 0xFFFF4500, 
    /* 5052  */ 0xFFFF4500, 
    /* 5053  */ 0xFFFF4400, 
    /* 5054  */ 0xFFFF4300, 
    /* 5055  */ 0xFFFF4300, 
    /* 5056  */ 0xFFFF4200, 
    /* 5057  */ 0xFFFF4100, 
    /* 5058  */ 0xFFFF4000, 
    /* 5059  */ 0xFFFF4000, 
    /* 5060  */ 0xFFFF3F00, 
    /* 5061  */ 0xFFFF3E00, 
    /* 5062  */ 0xFFFF3E00, 
    /* 5063  */ 0xFFFF3D00, 
    /* 5064  */ 0xFFFF3C00, 
    /* 5065  */ 0xFFFF3B00, 
    /* 5066  */ 0xFFFF3B00, 
    /* 5067  */ 0xFFFF3A00, 
    /* 5068  */ 0xFFFF3900, 
    /* 5069  */ 0xFFFF3900, 
    /* 5070  */ 0xFFFF3800, 
    /* 5071  */ 0xFFFF3700, 
    /* 5072  */ 0xFFFF3600, 
    /* 5073  */ 0xFFFF3600, 
    /* 5074  */ 0xFFFF3500, 
    /* 5075  */ 0xFFFF3400, 
    /* 5076  */ 0xFFFF3400, 
    /* 5077  */ 0xFFFF3300, 
    /* 5078  */ 0xFFFF3200, 
    /* 5079  */ 0xFFFF3000, 
    /* 5080  */ 0xFFFF2F00, 
    /* 5081  */ 0xFFFF2D00, 
    /* 5082  */ 0xFFFF2C00, 
    /* 5083  */ 0xFFFF2B00, 
    /* 5084  */ 0xFFFF2900, 
    /* 5085  */ 0xFFFF2800, 
    /* 5086  */ 0xFFFF2600, 
    /* 5087  */ 0xFFFF2500, 
    /* 5088  */ 0xFFFF2300, 
    /* 5089  */ 0xFFFF2200, 
    /* 5090  */ 0xFFFF2100, 
    /* 5091  */ 0xFFFF1F00, 
    /* 5092  */ 0xFFFF1E00, 
    /* 5093  */ 0xFFFF1C00, 
    /* 5094  */ 0xFFFF1B00, 
    /* 5095  */ 0xFFFF1900, 
    /* 5096  */ 0xFFFF1800, 
    /* 5097  */ 0xFFFF1700, 
    /* 5098  */ 0xFFFF1500, 
    /* 5099  */ 0xFFFF1400, 
    /* 5100  */ 0xFFFF1200, 
    /* 5101  */ 0xFFFF1100, 
    /* 5102  */ 0xFFFF0F00, 
    /* 5103  */ 0xFFFF0E00, 
    /* 5104  */ 0xFFFF0D00, 
    /* 5105  */ 0xFFFF0B00, 
    /* 5106  */ 0xFFFF0A00, 
    /* 5107  */ 0xFFFF0800, 
    /* 5108  */ 0xFFFF0700, 
    /* 5109  */ 0xFFFF0500, 
    /* 5110  */ 0xFFFF0400, 
    /* 5111  */ 0xFFFF0300, 
    /* 5112  */ 0xFFFF0100, 
    /* 5113  */ 0xFFFF0000, 
    /* 5114  */ 0xFFFF0000, 
    /* 5115  */ 0xFFFF0000, 
    /* 5116  */ 0xFFFF0000, 
    /* 5117  */ 0xFFFF0000, 
    /* 5118  */ 0xFFFF0000, 
    /* 5119  */ 0xFFFF0000, 
    /* 5120  */ 0xFFFF0000, 
    /* 5121  */ 0xFFFF0000, 
    /* 5122  */ 0xFFFF0000, 
    /* 5123  */ 0xFFFF0000, 
    /* 5124  */ 0xFFFF0000, 
    /* 5125  */ 0xFFFF0000, 
    /* 5126  */ 0xFFFF0000, 
    /* 5127  */ 0xFFFF0000, 
    /* 5128  */ 0xFFFF0000, 
    /* 5129  */ 0xFFFF0000, 
    /* 5130  */ 0xFFFF0000, 
    /* 5131  */ 0xFFFF0000, 
    /* 5132  */ 0xFFFF0000, 
    /* 5133  */ 0xFFFF0000, 
    /* 5134  */ 0xFFFF0000, 
    /* 5135  */ 0xFFFF0000, 
    /* 5136  */ 0xFFFF0000, 
    /* 5137  */ 0xFFFF0000, 
    /* 5138  */ 0xFFFF0000, 
    /* 5139  */ 0xFFFF0000, 
    /* 5140  */ 0xFFFF0000, 
    /* 5141  */ 0xFFFF0000, 
    /* 5142  */ 0xFFFF0000, 
    /* 5143  */ 0xFFFF0000, 
    /* 5144  */ 0xFFFF0000, 
    /* 5145  */ 0xFFFF0000, 
    /* 5146  */ 0xFFFF0000, 
    /* 5147  */ 0xFFFF0000, 
    /* 5148  */ 0xFFFF0000, 
    /* 5149  */ 0xFFFF0000, 
    /* 5150  */ 0xFFFF0004, 
    /* 5151  */ 0xFFFF0007, 
    /* 5152  */ 0xFFFF000B, 
    /* 5153  */ 0xFFFF000E, 
    /* 5154  */ 0xFFFF0012, 
    /* 5155  */ 0xFFFF0015, 
    /* 5156  */ 0xFFFF0019, 
    /* 5157  */ 0xFFFF001D, 
    /* 5158  */ 0xFFFF0020, 
    /* 5159  */ 0xFFFF0024, 
    /* 5160  */ 0xFFFF0027, 
    /* 5161  */ 0xFFFF002B, 
    /* 5162  */ 0xFFFF002E, 
    /* 5163  */ 0xFFFF0032, 
    /* 5164  */ 0xFFFF0036, 
    /* 5165  */ 0xFFFF0039, 
    /* 5166  */ 0xFFFF003D, 
    /* 5167  */ 0xFFFF0040, 
    /* 5168  */ 0xFFFF0044, 
    /* 5169  */ 0xFFFF0047, 
    /* 5170  */ 0xFFFF004B, 
    /* 5171  */ 0xFFFF004F, 
    /* 5172  */ 0xFFFF0052, 
    /* 5173  */ 0xFFFF0056, 
    /* 5174  */ 0xFFFF0059, 
    /* 5175  */ 0xFFFF005D, 
    /* 5176  */ 0xFFFF0060, 
    /* 5177  */ 0xFFFF0064, 
    /* 5178  */ 0xFFFF0068, 
    /* 5179  */ 0xFFFF006B, 
    /* 5180  */ 0xFFFF006F, 
    /* 5181  */ 0xFFFF0072, 
    /* 5182  */ 0xFFFF0076, 
    /* 5183  */ 0xFFFF0079, 
    /* 5184  */ 0xFFFF007D, 
    /* 5185  */ 0xFFFF0080, 
    /* 5186  */ 0xFFFF0082, 
    /* 5187  */ 0xFFFF0084, 
    /* 5188  */ 0xFFFF0086, 
    /* 5189  */ 0xFFFF0088, 
    /* 5190  */ 0xFFFF008B, 
    /* 5191  */ 0xFFFF008D, 
    /* 5192  */ 0xFFFF008F, 
    /* 5193  */ 0xFFFF0091, 
    /* 5194  */ 0xFFFF0093, 
    /* 5195  */ 0xFFFF0095, 
    /* 5196  */ 0xFFFF0097, 
    /* 5197  */ 0xFFFF009A, 
    /* 5198  */ 0xFFFF009C, 
    /* 5199  */ 0xFFFF009E, 
    /* 5200  */ 0xFFFF00A0, 
    /* 5201  */ 0xFFFF00A2, 
    /* 5202  */ 0xFFFF00A4, 
    /* 5203  */ 0xFFFF00A6, 
    /* 5204  */ 0xFFFF00A9, 
    /* 5205  */ 0xFFFF00AB, 
    /* 5206  */ 0xFFFF00AD, 
    /* 5207  */ 0xFFFF00AF, 
    /* 5208  */ 0xFFFF00B1, 
    /* 5209  */ 0xFFFF00B3, 
    /* 5210  */ 0xFFFF00B5, 
    /* 5211  */ 0xFFFF00B8, 
    /* 5212  */ 0xFFFF00BA, 
    /* 5213  */ 0xFFFF00BC, 
    /* 5214  */ 0xFFFF00BE, 
    /* 5215  */ 0xFFFF00C0, 
    /* 5216  */ 0xFFFF00C2, 
    /* 5217  */ 0xFFFF00C4, 
    /* 5218  */ 0xFFFF00C7, 
    /* 5219  */ 0xFFFF00C9, 
    /* 5220  */ 0xFFFF00CB, 
    /* 5221  */ 0xFFFF00CD, 
    /* 5222  */ 0xFFFF00CE, 
    /* 5223  */ 0xFFFF00CF, 
    /* 5224  */ 0xFFFF00D1, 
    /* 5225  */ 0xFFFF00D2, 
    /* 5226  */ 0xFFFF00D4, 
    /* 5227  */ 0xFFFF00D5, 
    /* 5228  */ 0xFFFF00D7, 
    /* 5229  */ 0xFFFF00D8, 
    /* 5230  */ 0xFFFF00D9, 
    /* 5231  */ 0xFFFF00DB, 
    /* 5232  */ 0xFFFF00DC, 
    /* 5233  */ 0xFFFF00DE, 
    /* 5234  */ 0xFFFF00DF, 
    /* 5235  */ 0xFFFF00E1, 
    /* 5236  */ 0xFFFF00E2, 
    /* 5237  */ 0xFFFF00E3, 
    /* 5238  */ 0xFFFF00E5, 
    /* 5239  */ 0xFFFF00E6, 
    /* 5240  */ 0xFFFF00E8, 
    /* 5241  */ 0xFFFF00E9, 
    /* 5242  */ 0xFFFF00EB, 
    /* 5243  */ 0xFFFF00EC, 
    /* 5244  */ 0xFFFF00ED, 
    /* 5245  */ 0xFFFF00EF, 
    /* 5246  */ 0xFFFF00F0, 
    /* 5247  */ 0xFFFF00F2, 
    /* 5248  */ 0xFFFF00F3, 
    /* 5249  */ 0xFFFF00F5, 
    /* 5250  */ 0xFFFF00F6, 
    /* 5251  */ 0xFFFF00F7, 
    /* 5252  */ 0xFFFF00F9, 
    /* 5253  */ 0xFFFF00FA, 
    /* 5254  */ 0xFFFF00FC, 
    /* 5255  */ 0xFFFF00FD, 
    /* 5256  */ 0xFFFFFF7F, 
    /* 5257  */ 0xFF7FFFFF, 
    /* 5258  */ 0xFFFF7F7F, 
    /* 5259  */ 0xFFA5E5A5, 
    /* 5260  */ 0xFF999919, 
    /* 5261  */ 0xFF991999, 
    /* 5262  */ 0xFF199999, 
    /* 5263  */ 0xFFBFBFFF, 
    /* 5264  */ 0xFFFFCC7F, 
    /* 5265  */ 0xFFCCFFFF, 
    /* 5266  */ 0xFF66B2B2, 
    /* 5267  */ 0xFF84BF00, 
    /* 5268  */ 0xFFB24C66, 
    /* 5269  */ 0xFFB78C4C, 
    /* 5270  */ 0xFF8CB266, 
    /* 5271  */ 0xFF8C3F99, 
    /* 5272  */ 0xFFB27F7F, 
    /* 5273  */ 0xFFFF7F7F, 
    /* 5274  */ 0xFFFFBFDD, 
    /* 5275  */ 0xFF3FFFBF, 
    /* 5276  */ 0xFFBFFF3F, 
    /* 5277  */ 0xFF337FCC, 
    /* 5278  */ 0xFFD8D8FF, 
    /* 5279  */ 0xFFD8337F, 
    /* 5280  */ 0xFFBA8C84, 
    /* 5281  */ 0xFFD9FFFF, 
    /* 5282  */ 0xFFCC7FFF, 
    /* 5283  */ 0xFFC2FF00, 
    /* 5284  */ 0xFFFFB5B5, 
    /* 5285  */ 0xFFB3FFFF, 
    /* 5286  */ 0xFFB3E3F5, 
    /* 5287  */ 0xFFAB5CF2, 
    /* 5288  */ 0xFF8AFF00, 
    /* 5289  */ 0xFFBFA6A6, 
    /* 5290  */ 0xFFF0C8A0, 
    /* 5291  */ 0xFFFF7F00, 
    /* 5292  */ 0xFF1FF01F, 
    /* 5293  */ 0xFF7FD1E3, 
    /* 5294  */ 0xFF8F3FD4, 
    /* 5295  */ 0xFF3DFF00, 
    /* 5296  */ 0xFFE6E6E6, 
    /* 5297  */ 0xFFBFC2C7, 
    /* 5298  */ 0xFFA6A6AB, 
    /* 5299  */ 0xFF8A99C7, 
    /* 5300  */ 0xFF9C7AC7, 
    /* 5301  */ 0xFFE06633, 
    /* 5302  */ 0xFFF090A0, 
    /* 5303  */ 0xFF50D050, 
    /* 5304  */ 0xFFC87F33, 
    /* 5305  */ 0xFF7D7FB0, 
    /* 5306  */ 0xFFC28F8F, 
    /* 5307  */ 0xFF668F8F, 
    /* 5308  */ 0xFFBD7FE3, 
    /* 5309  */ 0xFFFFA100, 
    /* 5310  */ 0xFFA62929, 
    /* 5311  */ 0xFF5CB8D1, 
    /* 5312  */ 0xFF702EB0, 
    /* 5313  */ 0xFF00FF00, 
    /* 5314  */ 0xFF94FFFF, 
    /* 5315  */ 0xFF94E0E0, 
    /* 5316  */ 0xFF73C2C9, 
    /* 5317  */ 0xFF54B5B5, 
    /* 5318  */ 0xFF3B9E9E, 
    /* 5319  */ 0xFF248F8F, 
    /* 5320  */ 0xFF0A7D8C, 
    /* 5321  */ 0xFF006985, 
    /* 5322  */ 0xFFC0C0C0, 
    /* 5323  */ 0xFFFFD98F, 
    /* 5324  */ 0xFFA67573, 
    /* 5325  */ 0xFF667F7F, 
    /* 5326  */ 0xFF9E63B5, 
    /* 5327  */ 0xFFD47A00, 
    /* 5328  */ 0xFF940094, 
    /* 5329  */ 0xFF429EB0, 
    /* 5330  */ 0xFF57178F, 
    /* 5331  */ 0xFF00C900, 
    /* 5332  */ 0xFF70D4FF, 
    /* 5333  */ 0xFFFFFFC7, 
    /* 5334  */ 0xFFD9FFC7, 
    /* 5335  */ 0xFFC7FFC7, 
    /* 5336  */ 0xFFA3FFC7, 
    /* 5337  */ 0xFF8FFFC7, 
    /* 5338  */ 0xFF61FFC7, 
    /* 5339  */ 0xFF45FFC7, 
    /* 5340  */ 0xFF30FFC7, 
    /* 5341  */ 0xFF1FFFC7, 
    /* 5342  */ 0xFF00FF9C, 
    /* 5343  */ 0xFF00E675, 
    /* 5344  */ 0xFF00D452, 
    /* 5345  */ 0xFF00BF38, 
    /* 5346  */ 0xFF00AB24, 
    /* 5347  */ 0xFF4DC2FF, 
    /* 5348  */ 0xFF4DA6FF, 
    /* 5349  */ 0xFF2194D6, 
    /* 5350  */ 0xFF267DAB, 
    /* 5351  */ 0xFF266696, 
    /* 5352  */ 0xFF175487, 
    /* 5353  */ 0xFFD0D0E0, 
    /* 5354  */ 0xFFFFD123, 
    /* 5355  */ 0xFFB8B8D0, 
    /* 5356  */ 0xFFA6544D, 
    /* 5357  */ 0xFF575961, 
    /* 5358  */ 0xFF9E4FB5, 
    /* 5359  */ 0xFFAB5C00, 
    /* 5360  */ 0xFF754F45, 
    /* 5361  */ 0xFF428296, 
    /* 5362  */ 0xFF420066, 
    /* 5363  */ 0xFF007D00, 
    /* 5364  */ 0xFF70ABFA, 
    /* 5365  */ 0xFF00BAFF, 
    /* 5366  */ 0xFF00A1FF, 
    /* 5367  */ 0xFF008FFF, 
    /* 5368  */ 0xFF007FFF, 
    /* 5369  */ 0xFF006BFF, 
    /* 5370  */ 0xFF545CF2, 
    /* 5371  */ 0xFF785CE3, 
    /* 5372  */ 0xFF8A4FE3, 
    /* 5373  */ 0xFFA136D4, 
    /* 5374  */ 0xFFB31FD4, 
    /* 5375  */ 0xFFB31FBA, 
    /* 5376  */ 0xFFB30DA6, 
    /* 5377  */ 0xFFBD0D87, 
    /* 5378  */ 0xFFC70066, 
    /* 5379  */ 0xFFCC0059, 
    /* 5380  */ 0xFFD1004F, 
    /* 5381  */ 0xFFD90045, 
    /* 5382  */ 0xFFE00038, 
    /* 5383  */ 0xFFE6002E, 
    /* 5384  */ 0xFFEB0026, 
    /* 5385  */ 0xFFE5E5E5 
  };

  static int getRGB(int color) {
    if (moreColors != null) {
      Integer key = Integer.valueOf(color);
      Integer c = moreColors.get(key);
      if (c != null)
        return c.intValue();
    }
    if (color < colors.length)
      return (colors[color]);
    return 0;
  }
  
  private final static Map<Integer, Integer> moreColors = new Hashtable<Integer, Integer>();
  
  static void addColor(Integer id, int value) {
    moreColors.put(id, Integer.valueOf(value));
  }

  /**
   * All settings that Jmol uses should go here
   * 
   * @param i
   * @param pymolVersion
   * @return setting or 0
   */
   static float getDefaultSetting(int i, int pymolVersion) {
    switch (i) {
    case sphere_color:
    case cartoon_color:
    case ellipsoid_color:
    case ribbon_color:
    case line_color:
    case dot_color:
    case stick_color:
    case surface_color:
    case dash_color:
    case mesh_color:
    case cartoon_putty_quality:
    case label_distance_digits:
    case label_angle_digits:
    case label_dihedral_digits:
    case two_sided_lighting:
      return -1;
    case ray_pixel_scale:
    case ellipsoid_scale:
    case sphere_scale:
    case mesh_width:
    case cartoon_ladder_mode:
    case clamp_colors:
    case frame:
    case state:
    case depth_cue:
    case fog:
    case cartoon_round_helices:
    case label_digits:
    case ribbon_sampling:
      return 1;
    case sphere_transparency:
    case ellipsoid_transparency:
    case ribbon_transparency:
    case nonbonded_transparency:
    case cartoon_transparency:
    case stick_transparency:
    case transparency:
    case bg_rgb:
    case cartoon_cylindrical_helices:
    case surface_mode:
    case surface_solvent:
    case all_states:
    case valence:
    case cgo_transparency:
    case cartoon_fancy_helices:
    case cartoon_putty_transform:
    case dump_binary:
    case orthoscopic:
    case ribbon_radius:
    case ribbon_smooth:
    case sphere_solvent:
    case surface_carve_cutoff:
      return 0;
    case cartoon_loop_radius:
      return 0.2F;
    case cartoon_rect_length:
      return 1.4F;
    case nonbonded_size:
      return 0.25F;
    case fog_start:
      return 0.45f;
    case label_size:
      return 14;
    case label_color:
      return -6;
    case label_font_id:
      return 5;
    case transparency_mode:
    case cartoon_putty_range:
    case cartoon_tube_cap:
      return 2;
    case cartoon_nucleic_acid_mode:
    case cartoon_putty_scale_max:
      return 4;
    case cartoon_putty_radius:
      return 0.4f;
    case cartoon_putty_scale_min:
      return 0.6f;
    case cartoon_putty_scale_power:
      return 1.5f;
    case cartoon_tube_radius:
      return 0.5f;
    case solvent_radius:
      return 1.4f;
    case dash_width:
      return 2.5f;
    case line_width:
      return 1.49f;
    case ribbon_width:
      return 3;
    case field_of_view:
      return 20;
    case movie_fps:
      return 30;
    case stick_radius:
      return 0.25f;
    case cartoon_oval_length:
      return 1.2f;
    case cartoon_oval_width:
      return 0.25f;
    default:
      Logger.error("PyMOL " + pymolVersion + " default float setting not found: " + i);
      return 0;
    }
  }

  static P3 getDefaultSettingPt(int i, int pymolVersion, P3 pt) {
    switch (i) {
    case label_position:
      pt.set(0,  0,  0.75f);
      break;
    default:
      Logger.error("PyMOL " + pymolVersion + " default point setting not found: " + i);
      break;
    }
    return pt;
  }

  static String getDefaultSettingS(int i, int pymolVersion) {
    switch (i) {
    case surface_carve_selection:
      break;
    default:
      Logger.info("PyMOL " + pymolVersion + " does not have String setting " + i);
      break;
    }
    return "";
  }

  /////// binary AtomInfo structures ////////////
  
  final static int LEN = 0;
  final static int RESV = 1;
  final static int CUSTOMTYPE = 2;
  final static int PRIORITY = 3;
  
  final static int BFACTOR = 4;
  final static int OCCUPANCY = 5;
  final static int VDW = 6;
  final static int PARTIALCHARGE = 7;
  
  //final static int SELENTRY = 8;
  final static int COLOR = 9;
  final static int ID = 10;
  final static int FLAGS = 11;
  //final static int TEMP1 = 12;
  final static int UNIQUEID = 13;
  final static int DISCRETESTATE = 14;
  
  final static int ELECRADIUS = 15;
  
  final static int RANK = 16;
  final static int TEXTTYPE = 17;
  final static int CUSTOM = 18;
  final static int LABEL = 19;
  final static int VISREP = 20;
  
  final static int HETATM = 21;
  final static int BONDED = 22;
  //final static int DELETEFLAG = 23;
  final static int MASK = 24;
  final static int HBDONOR = 25;
  final static int HBACCEPT = 26;
  final static int HASSETTING = 27;
  
  final static int FORMALCHARGE = 28;
  final static int MMSTEREO = 29;
  final static int CARTOON = 30;
  final static int GEOM = 31;
  final static int VALENCE = 32;
  final static int PROTONS = 33;
  
  final static int CHAIN = 34;
  final static int SEGI = 35;
  final static int NAME = 36;
  final static int ELEM = 37;
  final static int RESI = 38; // not 181
  final static int SSTYPE = 39;
  final static int ALTLOC = 40;
  final static int RESN = 41;
  final static int INSCODE = 42; // 181 only
  final static int CHEMFLAG = 43;
  final static int PROTEKTED = 44;
  final static int ANISOU = 45; // not 177
  
  final static int HETMASK = 46;
  final static int BONMASK = 47;
  final static int MASMASK = 48;
  final static int HBDMASK = 49;
  final static int HBAMASK = 50;
  final static int SETMASK = 51;
  
  //1.7.6 type
  final static int[] v176 = { LEN, 164,
    RESV,           0,
    CUSTOMTYPE,     4,
    PRIORITY,       8,
    BFACTOR,       12,
    OCCUPANCY,     16,
    VDW,           20,
    PARTIALCHARGE, 24,
    //SELENTRY,      28,
    COLOR,         32,
    ID,            36,
    FLAGS,         40,
    //TEMP1,         44,
    UNIQUEID,      48,
    DISCRETESTATE, 52,
    ELECRADIUS,    56,
    RANK,          60,
    TEXTTYPE,     -64,
    CUSTOM,       -68,
    LABEL,        -72,
    VISREP,        76,
    FORMALCHARGE,  80,
    //STEREO,       81,
    MMSTEREO,      82,
    CARTOON,       83,
    HETATM,        84,
    BONDED,        85,
    CHEMFLAG,      86,
    GEOM,          87,
    
    VALENCE,       88,
    //DELETEFLAG,      89,
    MASK,          90,
    PROTEKTED,     91,
    
    PROTONS,       92,
    HBDONOR,       93,
    HBACCEPT,      94,
    HASSETTING,    95,

    HETMASK,     0x01, 
    BONMASK,     0x01,
    MASMASK,     0x01,
    HBDMASK,     0x01,
    HBAMASK,     0x01,
    SETMASK,     0x01,
    
    CHAIN,         96,
    SEGI,         100, // 5
    NAME,         105, // 5
    ELEM,         110, // 5
    RESI,         115, // 6
    //HASPROP,      121,
    SSTYPE,       122,
    ALTLOC,       124,
    
    
    RESN,         126,
    ANISOU,       132,
    //OLDID,        156
    //PROPID,       160, // 4  
  };

//  typedef struct AtomInfoType_1_7_6 {
//    int resv;
//    int customType;
//    int priority;
//    float b, q, vdw, partialCharge;
//    int selEntry;
//    int color;
//    int id;                       // PDB ID
//    unsigned int flags;
//    int temp1;                    /* kludge fields - to remove */
//    int unique_id;                /* introduced in version 0.77 */
//    int discrete_state;           /* state+1 for atoms in discrete objects */
//    float elec_radius;            /* radius for PB calculations */
//    int rank;
//    int textType;
//    int custom;
//    int label;
//    int visRep;                   /* bitmask for all reps */
//
//    /* be careful not to write at these as (int*) */
//
//    signed char formalCharge;     // values typically in range -2..+2
//    signed char stereo;           /* for 2D representation */
//    signed char mmstereo;           /* from MMStereo */
//    signed char cartoon;          /* 0 = default which is auto (use ssType) */
//
//    // boolean flags
//    signed char hetatm;
//    signed char bonded;
//    signed char chemFlag;         // 0,1,2
//    signed char geom;             // cAtomInfo*
//
//    signed char valence;
//
//    // boolean flags
//    signed char deleteFlag;
//    signed char masked;
//
//    signed char protekted;        // 0,1,2
//
//    signed char protons;          /* atomic number */
//
//    // boolean flags
//    signed char hb_donor;
//    signed char hb_acceptor;
//    signed char has_setting;      /* setting based on unique_id */
//
//    int chain;
//    SegIdent segi;
//    AtomName name;
//    ElemName elem;                // redundant with "protons" ?
//    ResIdent resi;
//    char has_prop;
//    SSType ssType;                /* blank or 'L' = turn/loop, 'H' = helix, 'S' = beta-strand/sheet */
//    Chain alt;
//    ResName resn;
//
//    // replace with pointer?
//    float U11, U22, U33, U12, U13, U23;
//
//    int oldid; // for undo
//
//    int prop_id;
  
  final static int[] v177 = {LEN, 144,
  //  typedef struct AtomInfoType_1_7_7 {
  //    union {
  //      float * anisou;               // only allocate with get_anisou
  //      int64_t dummyanisou;
  //    };
    RESV, 8,
  //    int resv;
    CUSTOMTYPE, 12,
  //    int customType;
    PRIORITY, 16,
  //    int priority;
    BFACTOR,   20,
    OCCUPANCY, 24,
    VDW,       28,
    PARTIALCHARGE, 32,
  //    float b, q, vdw, partialCharge;
    //SELENTRY, 36;
  //    int selEntry;
    COLOR, 40,
  //    int color;
    ID,    44,
  //    int id;                       // PDB ID
    FLAGS, 48,
  //    unsigned int flags;
    //TEMP1, 52,
  //    int temp1;                    /* kludge fields - to remove */
    UNIQUEID, 56,
  //    int unique_id;                /* introduced in version 0.77 */
    DISCRETESTATE, 60,
  //    int discrete_state;           /* state+1 for atoms in discrete objects */
    ELECRADIUS,    64,
  //    float elec_radius;            /* radius for PB calculations */
    RANK,          68,
  //    int rank;
    TEXTTYPE,      -72,
  //    int textType;
    CUSTOM,        -76,
  //    int custom;
    LABEL,         -80,
  //    int label;
    VISREP,         84,
  //    int visRep;                   /* bitmask for all reps */
    //OLDID,          88,
  //    int oldid;                    // for undo
    //PROPID,           92,
  //    int prop_id;
  //
    HETATM,      96, //  bool hetatm : 1;
    BONDED,      96, //  bool bonded : 1;
    // deleteFlag
    MASK,        96, //  bool masked : 1;
    
    HBDONOR,     97, //  bool hb_donor : 1;
    HBACCEPT,    97, //  bool hb_acceptor : 1;
    HASSETTING,  97, //  bool has_setting : 1;      /* setting based on unique_id */
    //HASPROP,     97,
    
    HETMASK,    0x01, 
    BONMASK,    0x02,
    // deleteFlag 0x04,
    MASMASK,    0x08,
    
    HBDMASK,    0x01,
    HBAMASK,    0x02,
    SETMASK,    0x04,
    // hasprop 0x08

  //    // boolean flags
  //    bool hetatm : 1;
  //    bool bonded : 1;
  //    bool deleteFlag : 1;
  //    bool masked : 1;
    
  //    bool hb_donor : 1;
  //    bool hb_acceptor : 1;
  //    bool has_setting : 1;      /* setting based on unique_id */
  //    bool has_prop : 1;
  //
  //    /* be careful not to write at these as (int*) */
  //
    FORMALCHARGE, 98,
  //    signed char formalCharge;     // values typically in range -2..+2
    MMSTEREO,     99,
  //    signed char mmstereo;           /* from MMStereo */
    CARTOON,     100,
  //    signed char cartoon;          /* 0 = default which is auto (use ssType) */
    GEOM,        101,
  //    signed char geom;             // cAtomInfo*
    VALENCE,     102,
  //    signed char valence;          // 0-4
    PROTONS,     103,
  //    signed char protons;          /* atomic number */
    CHAIN,       104,
  //    int chain;
    SEGI,        108, // 5
  //    SegIdent segi;
    NAME,        113, // 5
  //    AtomName name;
    ELEM,        118, // 5
  //    ElemName elem;                // redundant with "protons" ?
    RESI,        123, // 6
  //    ResIdent resi;
    SSTYPE,      129,
  //    SSType ssType;                /* blank or 'L' = turn/loop, 'H' = helix, 'S' = beta-strand/sheet */
    ALTLOC,      131,
  //    Chain alt;
    RESN,        133,           
  //    ResName resn; // 6
    //STEREO     139,
  //    unsigned char stereo : 2;     // 0-3 Only for SDF (MOL) format in/out
    CHEMFLAG,    140,
  //    unsigned char chemFlag : 2;   // 0,1,2
    PROTEKTED,   141
  //    unsigned char protekted : 2;  // 0,1,2
  // padding,    142  // 2
  //
  };

  final static int[] v181 = { LEN, 120,  
  //  struct AtomInfoType_1_8_1 {
    
    ANISOU,       0,  //  short anisou[6];
    SEGI,        -12, //  lexidx_t segi;
    CHAIN,       -16, //  lexidx_t chain;
    RESN,        -20, //  lexidx_t resn;
    NAME,        -24, //  lexidx_t name;
    TEXTTYPE,    -28, //  lexidx_t textType;
    CUSTOM,      -32, //  lexidx_t custom;
    LABEL,       -36, //  lexidx_t label;
    
    RESV,         40, //  int resv;
    CUSTOMTYPE,   44, //  int customType;
    PRIORITY,     48, //  int priority;
    BFACTOR,      52, //  float b;
    OCCUPANCY,    56, //  float q;
    VDW,          60, //  float vdw;
    PARTIALCHARGE,64, //  float partialCharge;
    COLOR,        68, //  int color;
    ID,           72, //  int id;                       // PDB ID
    
    FLAGS,        76, //  unsigned int flags;
    UNIQUEID,     80, //  int unique_id;                /* introduced in version 0.77 */
    DISCRETESTATE,84, //  int discrete_state;           /* state+1 for atoms in discrete objects */
    ELECRADIUS,   88, //  float elec_radius;            /* radius for PB calculations */
    RANK,         92, //  int rank;
    VISREP,       96, //  int visRep;                   /* bitmask for all reps */
    
    HETATM,      100, //  bool hetatm : 1;
    BONDED,      100, //  bool bonded : 1;
    MASK,        100, //  bool masked : 1;
    HBDONOR,     100, //  bool hb_donor : 1;
    HBACCEPT,    100, //  bool hb_acceptor : 1;
    HASSETTING,  100, //  bool has_setting : 1;      /* setting based on unique_id */
    
    HETMASK,    0x01, 
    BONMASK,    0x02,
    MASMASK,    0x04,
    HBDMASK,    0x08,
    HBAMASK,    0x10,
    SETMASK,    0x20,
    
    FORMALCHARGE,101, //  signed char formalCharge;     // values typically in range -2..+2
    CARTOON,     102, //  signed char cartoon;          /* 0 = default which is auto (use ssType) */
    GEOM,        103, //  signed char geom;             // cAtomInfo*
    VALENCE,     104, //  signed char valence;          // 0-4
    PROTONS,     105, //  signed char protons;          /* atomic number */
    INSCODE,     106, //  char inscode;
    
    ELEM,        107, // [5] ElemName elem;               // redundant with "protons" ?
    SSTYPE,      112, // [2] SSType ssType;               /* blank or 'L' = turn/loop, 'H' = helix, 'S' = beta-strand/sheet */
    ALTLOC,      114, // [2] Chain alt;
    MMSTEREO,    116, //  unsigned char stereo : 2;     // 0-3 Only for SDF (MOL) format in/out
    CHEMFLAG,    117, //  unsigned char chemFlag : 2;   // 0,1,2
    PROTEKTED,   118, //  unsigned char protekted : 2;  // 0,1,2
    // padding   119, // 1
  };

  static int[] getVArray(int version) {
    int[] va = null;
    int[] varray = null;
    switch (version) {
    case 176:
      va = v176;
      break;
    case 177:
      va = v177;
      break;
    case 181:
      va = v181;
      break;
    }
    if (va != null) {
      varray = new int[60];
      for (int i = 0; i < va.length;)
        varray[va[i++]] = va[i++];
    }
    return varray;
  }

  /////// binary BondInfo structures ////////////

  static final int BATOM1 = 1;
  static final int BATOM2 = 2;
  static final int BORDER = 3;
  static final int BID    = 4;
  static final int BUNIQUEID = 5;
  static final int BHASSETTING = 6;
  
  final static int[] v176b = { LEN, 32,
    BATOM1, 0,
    BATOM2, 4,
    BORDER, 8,
    BID,   12,
    BUNIQUEID, 16,
    //    BTEMP1,    20,
    //    BSTEREO,   24,  // short
    BHASSETTING, 26,    // short lower byte
    // null byte,    27 // short higher byte
    //    BOLDID,      28
  };
  
  //typedef struct BondType_1_7_6 {
  //int index[2];
  //int order;
  //int id;
  //int unique_id;
  //int temp1;
  //short int stereo;             /* to preserve 2D rep */
  //short int has_setting;        /* setting based on unique_id */
  //int oldid;
  //} BondType_1_7_6;
    

    
  final static int[] v177b = { LEN, 24,
    BATOM1, 0,
    BATOM2, 4,
    BID,    8,
    BUNIQUEID, 12,
    //    BOLDID,      16
    BORDER, 20,           // byte
    //    BTEMP1,    21,  // byte
    //    BSTEREO,   22,  // byte
    BHASSETTING, 23,      // byte
    
  };
  
  
//
//typedef struct BondType_1_7_7 {
//int index[2];
//int id;
//int unique_id;
//int oldid;
//signed char order;    // 0-4
//signed char temp1;    // bool? where used?
//signed char stereo;   // 0-6 Only for SDF (MOL) format in/out
//bool has_setting;     /* setting based on unique_id */
//} BondType_1_7_7;
//

  final static int[] v181b = { LEN, 20,
    BATOM1, 0,
    BATOM2, 4,
    BID,    8,
    BUNIQUEID, 12,
    BORDER, 16,           // byte
    //    BSTEREO,   17,  // byte
    BHASSETTING, 18,      // byte    
    //    padding, 19,    // byte
  };
  
  
///*
//* This is not identical to the 1.8.2 BondType, it's missing all members
//* which are not relevant or unsupported with pse_binary_dump (oldid, temp1)
//*/
//struct BondType_1_8_1 {
//int index[2];
//int id;
//int unique_id;
//signed char order;    // 0-4
//signed char stereo;   // 0-6 Only for SDF (MOL) format in/out
//bool has_setting;     /* setting based on unique_id */
//};


  static int[] getVArrayB(int version) {
    int[] va = null;
    int[] varray = null;
    switch (version) {
    case 176:
      va = v176b;
      break;
    case 177:
      va = v177b;
      break;
    case 181:
      va = v181b;
      break;
    }
    if (va != null) {
      varray = new int[10];
      for (int i = 0; i < va.length;)
        varray[va[i++]] = va[i++];
    }
    return varray;
  }
}
