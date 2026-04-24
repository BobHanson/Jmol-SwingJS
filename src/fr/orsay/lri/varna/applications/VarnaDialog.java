package fr.orsay.lri.varna.applications;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;

/**
 * This class is for the GUI support? Maybe not necessary.
 */
public class VarnaDialog extends JDialog {


  public VarnaDialog() {
    init();
  }

  
private String _current_feature = "pairs";
private String _cache_key;
private String _cache_data;
private List<String> _items_all = new ArrayList<>();
private List<String> _items_filtered = new ArrayList<>();
private int _page = 0;


//
//self.obj_combo = JComboBox()
//self.obj_combo.currentIndexChanged.connect(self._on_object_changed)
//
//self.state_combo = JComboBox()
//self.count_btn = JButton("count")
//self.count_btn.clicked.connect(self._count_states_clicked)
//
//self.exe_edit = JTextField("x3dna-dssr")
//


  private void init() {
    setTitle("DSSR GUI");
    setSize(1020, 690);
    setLayout(new GridLayout());
    
    
  }
  
  
  
  
  
//        self.color_edit = JTextField("auto")
//        self.name_edit = JTextField("")
//
//        self.hb_mode_combo = JComboBox()
//        self.hb_mode_combo.addItems(["residue", "atom", "distance"])
//
//        self.precolor_cb = JCheckbox("gray precolor")
//        self.precolor_cb.setChecked(True)
//
//        self.display_cb = JCheckbox("display sticks")
//        self.display_cb.setChecked(False)
//
//        self.zoom_cb = JCheckbox("zoom")
//        self.zoom_cb.setChecked(True)
//
//        self.showinfo_cb = JCheckbox("show_info (pseudoknot)")
//        self.showinfo_cb.setChecked(False)
//
//        self.radius_spin = QtWidgets.QDoubleSpinBox()
//        self.radius_spin.setMinimum(0.01)
//        self.radius_spin.setMaximum(5.0)
//        self.radius_spin.setSingleStep(0.05)
//        self.radius_spin.setValue(0.25)
//
//        self.rna_btn = JButton("RNA only")
//        self.rna_btn.clicked.connect(self._make_rna_only)
//
//        self.status_label = JLabel("")
//        self.status_label.setWordWrap(True)
//
//        self.block_file_combo = JComboBox()
//        self.block_file_combo.setEditable(True)
//        self.block_file_combo.addItems(["face", "edge", "wc", "equal", "minor", "gray", "wc-minor"])
//
//        self.block_depth_spin = QtWidgets.QDoubleSpinBox()
//        self.block_depth_spin.setMinimum(0.01)
//        self.block_depth_spin.setMaximum(5.0)
//        self.block_depth_spin.setSingleStep(0.05)
//        self.block_depth_spin.setValue(0.5)
//
//        self.make_blocks_btn = JButton("make blocks")
//        self.make_blocks_btn.clicked.connect(self._make_blocks_clicked)
//
//        self.seq_btn = JButton("seq view")
//        self.seq_btn.clicked.connect(self._seq_view_clicked)
//
//        top.addWidget(JLabel("object"), 0, 0)
//        top.addWidget(self.obj_combo, 0, 1, 1, 2)
//        top.addWidget(self.rna_btn, 0, 3)
//
//        top.addWidget(JLabel("state"), 0, 4)
//        top.addWidget(self.state_combo, 0, 5)
//        top.addWidget(self.count_btn, 0, 6)
//
//        top.addWidget(JLabel("exe"), 1, 0)
//        top.addWidget(self.exe_edit, 1, 1, 1, 6)
//
//        top.addWidget(JLabel("color"), 2, 0)
//        top.addWidget(self.color_edit, 2, 1)
//        top.addWidget(JLabel("name"), 2, 2)
//        top.addWidget(self.name_edit, 2, 3)
//        top.addWidget(JLabel("hbonds_mode"), 2, 4)
//        top.addWidget(self.hb_mode_combo, 2, 5, 1, 2)
//
//        top.addWidget(JLabel("block_file"), 3, 0)
//        top.addWidget(self.block_file_combo, 3, 1)
//        top.addWidget(JLabel("block_depth"), 3, 2)
//        top.addWidget(self.block_depth_spin, 3, 3)
//        top.addWidget(self.make_blocks_btn, 3, 4, 1, 3)
//
//        opts = QtWidgets.QHBoxLayout()
//        opts.addWidget(self.precolor_cb)
//        opts.addWidget(self.display_cb)
//        opts.addWidget(self.zoom_cb)
//        opts.addWidget(self.showinfo_cb)
//        opts.addWidget(JLabel("stick_radius"))
//        opts.addWidget(self.radius_spin)
//        opts.addStretch(1)
//        root.addLayout(opts)
//
//        root.addWidget(self.status_label)
//
//        btn_area = QtWidgets.QWidget()
//        btn_grid = QtWidgets.QGridLayout(btn_area)
//        btn_grid.setContentsMargins(0, 0, 0, 0)
//        btn_grid.setHorizontalSpacing(6)
//        btn_grid.setVerticalSpacing(6)
//
//        self._feature_buttons = {}
//        cols = 6
//        for idx, feat in enumerate(FEATURE_ORDER):
//            b = JButton(feat)
//            b.setCheckable(True)
//            b.clicked.connect(self._make_feature_handler(feat))
//            r = idx // cols
//            c = idx % cols
//            btn_grid.addWidget(b, r, c)
//            self._feature_buttons[feat] = b
//        if "pairs" in self._feature_buttons:
//            self._feature_buttons["pairs"].setChecked(True)
//
//        root.addWidget(btn_area)
//
//        bar = QtWidgets.QHBoxLayout()
//
//        self.filter_edit = JTextField("")
//        self.filter_edit.setPlaceholderText("filter...")
//        self.filter_edit.textChanged.connect(self._on_filter_changed)
//
//        self.page_size_spin = QtWidgets.QSpinBox()
//        self.page_size_spin.setMinimum(50)
//        self.page_size_spin.setMaximum(5000)
//        self.page_size_spin.setValue(500)
//        self.page_size_spin.valueChanged.connect(self._on_page_size_changed)
//
//        self.prev_btn = JButton("Prev")
//        self.next_btn = JButton("Next")
//        self.prev_btn.clicked.connect(self._prev_page)
//        self.next_btn.clicked.connect(self._next_page)
//
//        self.page_label = JLabel("")
//
//        self.refresh_obj_btn = JButton("refresh objects")
//        self.refresh_obj_btn.clicked.connect(self.refresh_objects)
//        self.refresh_list_btn = JButton("refresh list")
//        self.refresh_list_btn.clicked.connect(self.refresh_list)
//
//        self.zoom_all_btn = JButton("zoom all")
//        self.zoom_all_btn.clicked.connect(self._zoom_all)
//
//        self.reset_view_btn = JButton("reset view")
//        self.reset_view_btn.clicked.connect(self._reset_view)
//
//        bar.addWidget(JLabel("filter"))
//        bar.addWidget(self.filter_edit, 2)
//        bar.addWidget(JLabel("max/page"))
//        bar.addWidget(self.page_size_spin, 0)
//        bar.addWidget(self.prev_btn)
//        bar.addWidget(self.next_btn)
//        bar.addWidget(self.page_label, 1)
//        bar.addWidget(self.refresh_obj_btn)
//        bar.addWidget(self.refresh_list_btn)
//        bar.addWidget(self.seq_btn)
//        bar.addWidget(self.zoom_all_btn)
//        bar.addWidget(self.reset_view_btn)
//        root.addLayout(bar)
//
//        report_bar = QtWidgets.QHBoxLayout()
//        self.report_btn = JButton("Generate RNA Report")
//        self.report_btn.clicked.connect(self._generate_rna_report_clicked)
//
//        self.make_all_sel_btn = JButton("make selections (all)")
//        self.make_all_sel_btn.clicked.connect(self._make_all_selections_clicked)
//
//        self.auto_color_btn = JButton("auto color")
//        self.auto_color_btn.clicked.connect(self._auto_color_clicked)
//
//        self.clear_report_btn = JButton("clear report")
//        self.clear_report_btn.clicked.connect(self._clear_report)
//
//        report_bar.addWidget(self.report_btn)
//        report_bar.addWidget(self.make_all_sel_btn)
//        report_bar.addWidget(self.auto_color_btn)
//        report_bar.addWidget(self.clear_report_btn)
//        report_bar.addStretch(1)
//        root.addLayout(report_bar)
//        self.list_widget = JList()
//        try:
//            self.list_widget.setSelectionMode(QtWidgets.QAbstractItemView.ExtendedSelection)
//        except Exception:
//            pass
//        self.list_widget.itemClicked.connect(self._on_item_clicked_preview)
//        self.list_widget.itemDoubleClicked.connect(self._on_item_double_clicked)
//        self.report_box = QtWidgets.QPlainTextEdit()
//        self.report_box.setReadOnly(True)
//        try:
//            self.report_box.setPlaceholderText("RNA Structure Summary will appear here...")
//        except Exception:
//            pass
//
//        split = JSplitPane()
//        split.setOrientation(QtCore.Qt.Horizontal)
//        split.addWidget(self.list_widget)
//        split.addWidget(self.report_box)
//        try:
//            split.setStretchFactor(0, 3)
//            split.setStretchFactor(1, 2)
//        except Exception:
//            pass
//
//        root.addWidget(split, 1)
//
//        self.refresh_objects()
//        self._update_state_combo()
//        self.refresh_list()
//
//    def _enable_seq_view(self):
//        try:
//            cmd.set("seq_view", 1)
//        except Exception:
//            try:
//                cmd.do("set seq_view, 1")
//            except Exception:
//                pass
//
//    def _seq_view_clicked(self):
//        self._enable_seq_view()
//        sel = self._get_object_text()
//        try:
//            cmd.select("sele", "(%s) and polymer.nucleic" % sel)
//        except Exception:
//            pass
//
//    def _make_feature_handler(self, feat):
//        def handler():
//            self._current_feature = feat
//            for k, b in self._feature_buttons.items():
//                b.setChecked(k == feat)
//            self.refresh_list()
//        return handler
//
//    def _on_object_changed(self):
//        self._update_state_combo()
//
//    def _count_states_clicked(self):
//        obj = self._get_object_text()
//        try:
//            n = int(cmd.count_states(obj))
//        except Exception:
//            n = 0
//        self._update_state_combo(force_n=n)
//        try:
//            QtWidgets.QMessageBox.information(self, "count_states", "count_states %s = %d" % (obj, n))
//        except Exception:
//            pass
//
//    def _get_object_text(self):
//        txt = self.obj_combo.currentText().strip() if self.obj_combo.count() else ""
//        return txt if txt else "all"
//
//    def _update_state_combo(self, force_n= null):
//        obj = self._get_object_text()
//        try:
//            n = int(force_n) if force_n is not None else int(cmd.count_states(obj))
//        except Exception:
//            n = 0
//
//        current = self.state_combo.currentData() if self.state_combo.count() else None
//        self.state_combo.clear()
//
//        self.state_combo.addItem("current", -1)
//        if n and n > 1:
//            for s in range(1, n + 1):
//                self.state_combo.addItem(str(s), s)
//
//        if current is not None:
//            idx = self.state_combo.findData(current)
//            if idx >= 0:
//                self.state_combo.setCurrentIndex(idx)
//
//    def _get_state_value(self):
//        data = self.state_combo.currentData()
//        try:
//            data = int(data)
//        except Exception:
//            data = -1
//        if data == -1:
//            try:
//                return int(cmd.get_state())
//            except Exception:
//                return 1
//        return data
//
//    def refresh_objects(self):
//        try:
//            objs = cmd.get_object_list("enabled")
//            if not objs:
//                objs = cmd.get_object_list()
//        except Exception:
//            objs = []
//
//        current = self.obj_combo.currentText().strip() if self.obj_combo.count() else ""
//        self.obj_combo.clear()
//
//        if not objs:
//            self.obj_combo.addItem("all")
//        else:
//            for o in objs:
//                self.obj_combo.addItem(o)
//
//        if current:
//            i = self.obj_combo.findText(current)
//            if i >= 0:
//                self.obj_combo.setCurrentIndex(i)
//        else:
//            try:
//                enabled = cmd.get_object_list("enabled")
//            except Exception:
//                enabled = []
//            if len(enabled) == 1:
//                i = self.obj_combo.findText(enabled[0])
//                if i >= 0:
//                    self.obj_combo.setCurrentIndex(i)
//
//        self._update_state_combo()
//
//    def _zoom_all(self):
//        try:
//            cmd.zoom("all")
//        except Exception:
//            pass
//
//    def _reset_view(self):
//        apply_gray = True if self.precolor_cb.isChecked() else False
//        _clear_keep_molecules(apply_gray)
//        self.refresh_objects()
//        self.refresh_list()
//
//    def _unique_object_name(self, base):
//        base = str(base)
//        if not base:
//            base = "rna_only"
//        name = base
//        k = 1
//        while True:
//            try:
//                exists = name in cmd.get_object_list()
//            except Exception:
//                exists = False
//            if not exists:
//                return name
//            k += 1
//            name = "%s%d" % (base, k)
//
//    def _make_rna_only(self):
//        obj = self._get_object_text()
//        new_name = self._unique_object_name("rna_only")
//        try:
//            cmd.create(new_name, "(%s) and polymer.nucleic" % obj)
//        except Exception as e:
//            try:
//                QtWidgets.QMessageBox.critical(self, "RNA only", str(e))
//            except Exception:
//                pass
//            return
//
//        self.refresh_objects()
//        i = self.obj_combo.findText(new_name)
//        if i >= 0:
//            self.obj_combo.setCurrentIndex(i)
//        self.refresh_list()
//
//    def _clear_report(self):
//        try:
//            self.report_box.setPlainText("")
//        except Exception:
//            pass
//
//    def _get_dssr_context(self):
//        sel_obj = self._get_object_text()
//        exe = self.exe_edit.text().strip() or "x3dna-dssr"
//        st = self._get_state_value()
//        precolor_on = 1 if self.precolor_cb.isChecked() else 0
//        return sel_obj, exe, st, precolor_on
//
//    def _append_report(self, text):
//        try:
//            cur = self.report_box.toPlainText()
//        except Exception:
//            cur = ""
//        if cur:
//            out = cur.rstrip("\n") + "\n\n" + str(text).rstrip("\n") + "\n"
//        else:
//            out = str(text).rstrip("\n") + "\n"
//        try:
//            self.report_box.setPlainText(out)
//        except Exception:
//            pass
//
//    def _generate_rna_report_clicked(self):
//        sel_obj, exe, st, precolor_on = self._get_dssr_context()
//        try:
//            dssr_data = self._get_dssr_data(sel_obj, st, exe, precolor_on)
//            report = _format_rna_summary_text(dssr_data)
//            try:
//                self.report_box.setPlainText(report + "\n")
//            except Exception:
//                self._append_report(report)
//        except Exception as e:
//            msg = "RNA report error: %s" % str(e)
//            self._append_report(msg)
//            try:
//                QtWidgets.QMessageBox.critical(self, "RNA Report", msg)
//            except Exception:
//                pass
//
//    def _make_all_selections_clicked(self):
//        sel_obj, exe, st, precolor_on = self._get_dssr_context()
//        try:
//            dssr_data = self._get_dssr_data(sel_obj, st, exe, precolor_on)
//            made, skipped = self._make_all_selections(dssr_data, sel_obj)
//            self._append_report("Created selections: %s" % (" ".join(made) if made else "(none)"))
//            if skipped:
//                self._append_report("Skipped (empty): %s" % (" ".join(skipped)))
//        except Exception as e:
//            msg = "make selections error: %s" % str(e)
//            self._append_report(msg)
//            try:
//                QtWidgets.QMessageBox.critical(self, "make selections", msg)
//            except Exception:
//                pass
//
//    def _auto_color_clicked(self):
//        sel_obj, exe, st, precolor_on = self._get_dssr_context()
//        try:
//            dssr_data = self._get_dssr_data(sel_obj, st, exe, precolor_on)
//            self._make_all_selections(dssr_data, sel_obj)
//            self._apply_auto_colors()
//            self._append_report("Auto color applied: stems green, hairpins blue, pseudoknots red, aminors purple")
//        except Exception as e:
//            msg = "auto color error: %s" % str(e)
//            self._append_report(msg)
//            try:
//                QtWidgets.QMessageBox.critical(self, "auto color", msg)
//            except Exception:
//                pass
//
//    def _make_all_selections(self, dssr_data, obj_sel):
//        targets = [
//            ("pairs", "pairs_all"),
//            ("hairpins", "hairpins_all"),
//            ("stems", "stems_all"),
//            ("bulges", "bulges_all"),
//            ("junctions", "junctions_all"),
//            ("pseudoknot", "pseudoknots_all"),
//            ("aminors", "aminors_all"),
//            ("stacks", "stacks_all"),
//        ]
//
//        made = []
//        skipped = []
//
//        for feat, name in targets:
//            residues = _collect_residues_all(dssr_data, feat)
//            sel_core = _compact_sel_from_residues(residues)
//            if not sel_core:
//                skipped.append(name)
//                try:
//                    cmd.delete(name)
//                except Exception:
//                    pass
//                continue
//
//            expr = "((%s) and polymer.nucleic and (%s))" % (obj_sel, sel_core)
//            try:
//                cmd.select(name, expr)
//                made.append(name)
//            except Exception:
//                skipped.append(name)
//                try:
//                    cmd.delete(name)
//                except Exception:
//                    pass
//
//        return made, skipped
//
//    def _apply_auto_colors(self):
//        try:
//            cmd.color("green", "stems_all")
//        except Exception:
//            pass
//        try:
//            cmd.color("blue", "hairpins_all")
//        except Exception:
//            pass
//        try:
//            cmd.color("red", "pseudoknots_all")
//        except Exception:
//            pass
//        try:
//            cmd.color("purple", "aminors_all")
//        except Exception:
//            pass
//
//
//    def _big_object_warning(self, sel):
//        thresh = 250000
//        try:
//            n_atoms = int(cmd.count_atoms(sel))
//        except Exception:
//            n_atoms = 0
//        if n_atoms >= thresh:
//            self.status_label.setText("warning: large selection (%d atoms). recommended: use RNA only and avoid feature=nts/hbonds first." % n_atoms)
//        else:
//            self.status_label.setText("")
//
//    def _get_dssr_data(self, selection, state, exe, precolor_on):
//        import tempfile, os
//        cache_key = (str(selection), int(state), str(exe))
//        if self._cache_key == cache_key and self._cache_data is not None:
//            if int(precolor_on):
//                try:
//                    cmd.color("gray", selection)
//                except Exception:
//                    pass
//            return self._cache_data
//
//        tmp = tempfile.NamedTemporaryFile(suffix=".pdb", delete=False)
//        tmpfilepdb = tmp.name
//        tmp.close()
//
//        try:
//            cmd.save(tmpfilepdb, selection, state)
//            if int(precolor_on):
//                cmd.color("gray", selection)
//            data = run_dssr_json(tmpfilepdb, exe)
//        finally:
//            try:
//                os.remove(tmpfilepdb)
//            except OSError:
//                pass
//
//        self._cache_key = cache_key
//        self._cache_data = data
//        return data
//
//    def _on_filter_changed(self, _):
//        self._page = 0
//        self._render_list()
//
//    def _on_page_size_changed(self, _):
//        self._page = 0
//        self._render_list()
//
//    def _prev_page(self):
//        if self._page > 0:
//            self._page -= 1
//            self._render_list()
//
//    def _next_page(self):
//        pages = self._total_pages()
//        if self._page + 1 < pages:
//            self._page += 1
//            self._render_list()
//
//    def _total_pages(self):
//        page_size = int(self.page_size_spin.value())
//        total = len(self._items_filtered)
//        if page_size <= 0:
//            return 1
//        pages = (total + page_size - 1) // page_size
//        return max(1, pages)
//
//    def refresh_list(self):
//        sel = self._get_object_text()
//        feat = self._current_feature
//        exe = self.exe_edit.text().strip() or "x3dna-dssr"
//        st = self._get_state_value()
//        precolor_on = 1 if self.precolor_cb.isChecked() else 0
//
//        self._big_object_warning(sel)
//
//        self.list_widget.clear()
//        self.list_widget.addItem("loading...")
//        QtWidgets.QApplication.processEvents()
//
//        try:
//            dssr_data = self._get_dssr_data(sel, st, exe, precolor_on)
//
//            items = []
//
//            if feat == "pseudoknot":
//                dotbracket = _extract_dotbracket(dssr_data)
//                nts_list = dssr_data.get("nts", None)
//                if nts_list is None:
//                    raise CmdException("No nts found in DSSR output")
//
//                layers = parse_dotbracket_pseudoknots(dotbracket)
//                if not layers:
//                    raise CmdException("No pseudoknot layers found")
//
//                layer_keys = sorted(layers.keys())
//                for j, k in enumerate(layer_keys, 1):
//                    line = "%d: layer key=%s pairs=%d" % (j, str(k), len(layers[k]))
//                    items.append((j, line))
//
//            else:
//                if feat not in FEATURE_MAP:
//                    raise CmdException("Unknown feature "%s"" % feat)
//                json_key = FEATURE_MAP[feat]
//                feature_list = dssr_data.get(json_key, None)
//                if feature_list is None or not isinstance(feature_list, list) or len(feature_list) == 0:
//                    raise CmdException("No "%s" found in DSSR output" % json_key)
//
//                total = len(feature_list)
//                for i in range(total):
//                    line = _preview_entry(feat, feature_list[i], i + 1)
//                    items.append((i + 1, line))
//
//            self._items_all = items
//            self._page = 0
//            self._render_list()
//
//        except Exception as e:
//            self._items_all = []
//            self._items_filtered = []
//            self.list_widget.clear()
//            self.list_widget.addItem("ERROR: %s" % str(e))
//            try:
//                print("dssr_gui error: %s" % str(e))
//            except Exception:
//                pass
//
//    def _render_list(self):
//        q = self.filter_edit.text().strip().lower()
//        if q:
//            self._items_filtered = [it for it in self._items_all if q in it[1].lower()]
//        else:
//            self._items_filtered = list(self._items_all)
//
//        total_all = len(self._items_all)
//        total_f = len(self._items_filtered)
//
//        page_size = int(self.page_size_spin.value())
//        pages = self._total_pages()
//
//        if self._page >= pages:
//            self._page = max(0, pages - 1)
//
//        start = self._page * page_size
//        end = start + page_size
//        page_items = self._items_filtered[start:end]
//
//        self.list_widget.clear()
//        for idx, text in page_items:
//            it = JListItem(text)
//            it.setData(QtCore.Qt.UserRole, int(idx))
//            self.list_widget.addItem(it)
//
//        self.page_label.setText("items %d, filtered %d, page %d/%d" % (total_all, total_f, self._page + 1, pages))
//        self.prev_btn.setEnabled(self._page > 0)
//        self.next_btn.setEnabled(self._page + 1 < pages)
//
//        try:
//            self.setWindowTitle("DSSR GUI - %s" % self._current_feature)
//        except Exception:
//            pass
//
//    def _on_item_clicked_preview(self, item):
//        self._enable_seq_view()
//
//        data = item.data(QtCore.Qt.UserRole)
//        if data is None:
//            return
//        try:
//            idx = int(data)
//        except Exception:
//            return
//
//        sel_obj = self._get_object_text()
//        feat = self._current_feature
//        exe = self.exe_edit.text().strip() or "x3dna-dssr"
//        st = self._get_state_value()
//        precolor_on = 1 if self.precolor_cb.isChecked() else 0
//
//        try:
//            dssr_data = self._get_dssr_data(sel_obj, st, exe, precolor_on)
//            sel_str = _build_residue_sel_from_dssr(dssr_data, feat, idx)
//            cmd.select("sele", sel_str)
//            cmd.select("sele", "byres (sele) and polymer.nucleic")
//        except Exception as e:
//            try:
//                self.status_label.setText("preview error: %s" % str(e))
//            except Exception:
//                pass
//
//    def _on_item_double_clicked(self, item):
//        data = item.data(QtCore.Qt.UserRole)
//        if data is None:
//            return
//        try:
//            idx = int(data)
//        except Exception:
//            return
//
//        sel = self._get_object_text()
//        feat = self._current_feature
//        exe = self.exe_edit.text().strip() or "x3dna-dssr"
//        st = self._get_state_value()
//
//        nm = self.name_edit.text().strip()
//        if not nm:
//            nm = "%s%d" % (feat, idx)
//
//        col = self.color_edit.text().strip() or "auto"
//        precolor_on = 1 if self.precolor_cb.isChecked() else 0
//        display_on = 1 if self.display_cb.isChecked() else 0
//        zoom_on = 1 if self.zoom_cb.isChecked() else 0
//        showinfo_on = 1 if self.showinfo_cb.isChecked() else 0
//        radius = float(self.radius_spin.value())
//
//        hb_mode = self.hb_mode_combo.currentText().strip().lower()
//        dist_name = "dist_%s" % nm
//
//        try:
//            dssr(sel=sel, f=feat, i=idx, n=nm, q=0, si=showinfo_on, st=st,
//                 exe=exe, color=col, display=display_on, stick_radius=radius,
//                 do_zoom=zoom_on, pc=precolor_on, hbmode=hb_mode, distname=dist_name)
//        except Exception as e:
//            try:
//                QtWidgets.QMessageBox.critical(self, "DSSR GUI error", str(e))
//            except Exception:
//                pass
//            try:
//                print("dssr_gui select error: %s" % str(e))
//            except Exception:
//                pass
//
//    def _make_blocks_clicked(self):
//        self._enable_seq_view()
//
//        sel_obj = self._get_object_text()
//        exe = self.exe_edit.text().strip() or "x3dna-dssr"
//        st = self._get_state_value()
//
//        block_file = self.block_file_combo.currentText().strip() or "face"
//        block_depth = float(self.block_depth_spin.value())
//
//        base = self.name_edit.text().strip()
//        if not base:
//            base = "blk"
//
//        items = []
//        try:
//            items = list(self.list_widget.selectedItems())
//        except Exception:
//            items = []
//
//        dssr_data = None
//        if items:
//            try:
//                precolor_on = 1 if self.precolor_cb.isChecked() else 0
//                dssr_data = self._get_dssr_data(sel_obj, st, exe, precolor_on)
//            except Exception as e:
//                try:
//                    QtWidgets.QMessageBox.critical(self, "make blocks error", str(e))
//                except Exception:
//                    pass
//                return
//
//        made_names = []
//
//        try:
//            if items:
//                feat = self._current_feature
//                for it in items:
//                    data = it.data(QtCore.Qt.UserRole)
//                    if data is None:
//                        continue
//                    try:
//                        idx = int(data)
//                    except Exception:
//                        continue
//
//                    sel_str = _build_residue_sel_from_dssr(dssr_data, feat, idx)
//                    sel_for_block = "byres (%s) and polymer.nucleic" % sel_str
//
//                    obj_name = "%s_%s_%d" % (base, feat, idx)
//                    try:
//                        cmd.delete(obj_name)
//                    except Exception:
//                        pass
//
//                    dssr_block(selection=sel_for_block, state=st,
//                               block_file=block_file, block_depth=block_depth,
//                               name=obj_name, exe=exe, quiet=1)
//
//                    _DSSR_BLOCK_OBJECTS.add(obj_name)
//                    made_names.append(obj_name)
//
//            else:
//                try:
//                    n = int(cmd.count_atoms("sele"))
//                except Exception:
//                    n = 0
//                if n > 0:
//                    sel_for_block = "byres (sele) and polymer.nucleic"
//                else:
//                    sel_for_block = "(%s) and polymer.nucleic" % sel_obj
//
//                obj_name = "%s_sel" % base
//                try:
//                    cmd.delete(obj_name)
//                except Exception:
//                    pass
//
//                dssr_block(selection=sel_for_block, state=st,
//                           block_file=block_file, block_depth=block_depth,
//                           name=obj_name, exe=exe, quiet=1)
//
//                _DSSR_BLOCK_OBJECTS.add(obj_name)
//                made_names.append(obj_name)
//
//            if made_names and self.zoom_cb.isChecked():
//                try:
//                    cmd.zoom("(%s)" % " or ".join(made_names))
//                except Exception:
//                    try:
//                        cmd.zoom("all")
//                    except Exception:
//                        pass
//
//        except Exception as e:
//            try:
//                QtWidgets.QMessageBox.critical(self, "make blocks error", str(e))
//            except Exception:
//                pass
//            try:
//                print("make blocks error: %s" % str(e))
//            except Exception:
//                pass
//
//
//def dssr_gui():
//    global _DSSR_GUI_DIALOG
//    if QtWidgets is None or QtCore is None:
//        raise CmdException("Qt is not available in this PyMOL build")
//    if _DSSR_GUI_DIALOG is None:
//        _DSSR_GUI_DIALOG = _DSSRGuiDialog()
//    _DSSR_GUI_DIALOG.show()
//    _DSSR_GUI_DIALOG.raise_()
//    _DSSR_GUI_DIALOG.activateWindow()
//
//def __init_plugin__(app= null):
//    from pymol.plugins import addmenuitemqt
//    addmenuitemqt("DSSR", dssr_gui)
//
//cmd.extend("dssr_select", dssr_select)
//cmd.extend("dssr_gui", dssr_gui)
//cmd.extend("dssr_block", dssr_block)
//cmd.extend("dssr_seq", dssr_seq)
//
//cmd.auto_arg[0].update({"dssr_select": cmd.auto_arg[0]["zoom"]})
//cmd.auto_arg[0].update({"dssr_gui": cmd.auto_arg[0]["zoom"]})
//cmd.auto_arg[0].update({"dssr_block": cmd.auto_arg[0]["zoom"]})
//cmd.auto_arg[0].update({"dssr_seq": cmd.auto_arg[0]["zoom"]})
//
//try:
//    print("Loaded DSSR helper %s from: %s" % (__DSSR_PLUGIN_VERSION__, __file__))
//except Exception:
//    pass
//
//  
//  
//
}
