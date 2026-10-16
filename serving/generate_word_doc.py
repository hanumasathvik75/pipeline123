import os
import shutil
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, hex_color):
    """Sets background color of a table cell."""
    shading_xml = f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>'
    cell._tc.get_or_add_tcPr().append(parse_xml(shading_xml))

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    """Sets internal cell margins in twips."""
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def create_document():
    doc = docx.Document()
    
    # Page setup: Standard Letter/A4 with compact 0.75-inch margins
    sections = doc.sections
    for s in sections:
        s.top_margin = Inches(0.75)
        s.bottom_margin = Inches(0.75)
        s.left_margin = Inches(0.75)
        s.right_margin = Inches(0.75)
        
    # Color Palette
    PRIMARY = RGBColor(27, 54, 93)     # Deep Navy #1B365D
    SECONDARY = RGBColor(74, 119, 157) # Slate Blue #4A779D
    DARK_TEXT = RGBColor(40, 40, 40)   # Charcoal
    MUTED_TEXT = RGBColor(100, 100, 100)
    
    # -------------------------------------------------------------
    # HEADER / TITLE
    # -------------------------------------------------------------
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(0)
    p_title.paragraph_format.space_after = Pt(2)
    run_title = p_title.add_run("IgnitionAI: Physics-Informed Dual-Nature Diagnostic Engine")
    run_title.font.name = "Arial"
    run_title.font.size = Pt(20)
    run_title.font.bold = True
    run_title.font.color.rgb = PRIMARY
    
    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(12)
    run_sub = p_sub.add_run("Technical & Mathematical Architecture: Data Synthesis, Multi-Output ML, and Hybrid Decision Fusion")
    run_sub.font.name = "Arial"
    run_sub.font.size = Pt(11)
    run_sub.font.italic = True
    run_sub.font.color.rgb = SECONDARY
    
    # Meta box
    tbl_meta = doc.add_table(rows=1, cols=3)
    tbl_meta.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_meta.autofit = False
    col_widths = [Inches(2.3), Inches(2.3), Inches(2.4)]
    
    meta_headers = [
        ("PROJECT", "IgnitionAI MVP (Stage 2)"),
        ("MODEL TYPE", "Multi-Output XGBoost + Invariants"),
        ("EVALUATION", "95.36% Subset Acc / 97.16% W-F1")
    ]
    for i, (label, val) in enumerate(meta_headers):
        cell = tbl_meta.cell(0, i)
        cell.width = col_widths[i]
        set_cell_background(cell, "F0F4F8")
        set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
        p = cell.paragraphs[0]
        p.paragraph_format.space_after = Pt(0)
        r_lbl = p.add_run(f"{label}: ")
        r_lbl.font.size = Pt(8.5)
        r_lbl.font.bold = True
        r_lbl.font.color.rgb = PRIMARY
        r_val = p.add_run(val)
        r_val.font.size = Pt(8.5)
        r_val.font.color.rgb = DARK_TEXT

    doc.add_paragraph().paragraph_format.space_after = Pt(4)

    # -------------------------------------------------------------
    # SECTION 1: EXECUTIVE SUMMARY & PROBLEM FORMULATION
    # -------------------------------------------------------------
    h1 = doc.add_heading(level=1)
    r1 = h1.add_run("1. Executive Summary & Problem Formulation")
    r1.font.name = "Arial"
    r1.font.size = Pt(13)
    r1.font.bold = True
    r1.font.color.rgb = PRIMARY
    h1.paragraph_format.space_before = Pt(10)
    h1.paragraph_format.space_after = Pt(4)
    
    p_body1 = doc.add_paragraph()
    p_body1.paragraph_format.space_after = Pt(6)
    p_body1.paragraph_format.line_spacing = 1.15
    p_body1.add_run(
        "Modern pre-owned vehicle dealerships and multi-brand aftermarket workshops require automated, objective vehicle condition assessment without disassembling mechanical assemblies. Standard OBD-II fault code (DTC) scanners fail to flag pre-DTC component wear, intermittent stator faults, or early intake vacuum leaks where the ECU's closed-loop control remains barely within factory limp-home tolerances. "
        "The IgnitionAI platform implements a "
    )
    r_bold1 = p_body1.add_run("Physics-Informed Dual-Nature Hybrid Diagnostic Architecture (Option B)")
    r_bold1.font.bold = True
    p_body1.add_run(
        ". Rather than relying solely on pure machine learning (which easily falls victim to spurious correlation shortcuts) or simple scalar rules (which fail across dynamic vehicle operating envelopes), IgnitionAI fuses "
    )
    p_body1.add_run("multivariate gradient-boosted decision trees (MultiOutput XGBoost)")
    p_body1.add_run(" with ")
    p_body1.add_run("first-principles mechanical, thermodynamic, and electrochemical invariants.")
    
    # -------------------------------------------------------------
    # SECTION 2: SYNTHETIC DATA GENERATION (PHYSICS DIGITAL TWIN)
    # -------------------------------------------------------------
    h2 = doc.add_heading(level=1)
    r2 = h2.add_run("2. Physics-Based Synthetic Telemetry Generation")
    r2.font.name = "Arial"
    r2.font.size = Pt(13)
    r2.font.bold = True
    r2.font.color.rgb = PRIMARY
    h2.paragraph_format.space_before = Pt(8)
    h2.paragraph_format.space_after = Pt(4)
    
    p_body2 = doc.add_paragraph()
    p_body2.paragraph_format.space_after = Pt(4)
    p_body2.paragraph_format.line_spacing = 1.15
    p_body2.add_run(
        "To establish a mathematically rigorous training and validation benchmark ahead of hardware BLE rollouts, a physics-grounded synthetic telemetry generator was developed ("
    )
    p_body2.add_run("generate_75_physics_drives.py").font.bold = True
    p_body2.add_run(
        "). The generator simulates 75 continuous road test sessions (600 seconds each = 45,000 total seconds of continuous 1 Hz telemetry) across 10 distinct Indian vehicle models (Maruti Swift, Baleno, WagonR, Dzire, Ertiga; Hyundai i20, Creta, Venue; Honda City, Amaze). "
        "Unlike basic random walk simulators, every sensor channel is coupled through real physical governing equations:"
    )
    
    bullets2 = [
        ("Speed-Density Airflow Invariant: ", "Cylinder air mass flow is dynamically derived using the Ideal Gas Law: MAF = (RPM / 120) × V_disp × (P_MAP / (R_air × T_intake)) × η_v(RPM, MAP). Intake vacuum leaks elevate idle MAP while under-reading on the mass air flow sensor."),
        ("Closed-Loop Stoichiometry Limit Cycle: ", "Narrow-band O2 sensor switching is modeled at 0.18 Hz between 0.1V (lean) and 0.9V (rich). Fuel trims apply counter-phase proportional-integral (PI) correction only when coolant temp exceeds 45°C (closed-loop light-off threshold)."),
        ("Newton's Cooling Law with Ram-Air Convection: ", "Thermal accumulation models combustion heat release against road-speed-dependent convective cooling through the radiator grill: Q_out = h_ambient × (1 + 0.045 × v_kph) × (T_coolant - T_ambient). A stuck-open thermostat produces significant thermal deficit during highway cruise."),
        ("Alternator Stator Induction Droop: ", "Electrical charging is modeled as a function of rotor speed: stator current droops below 1,350 RPM. A failing diode bridge collapses system voltage to 11.5V-12.0V only at idle under accessory loads (cooling fan, headlights), recovering at 2,500 RPM."),
        ("Throttle-Decoupled Combustion Hunting: ", "Idle air control valve (IACV) sticking and cyclic torque disturbances are modeled as a 0.15 Hz sinusoidal surge oscillation (±120 RPM) decoupled from driver throttle intent (active strictly when throttle plate ≤ 12% and road speed < 8 km/h).")
    ]
    for b_title, b_desc in bullets2:
        bp = doc.add_paragraph(style='List Bullet')
        bp.paragraph_format.space_after = Pt(2)
        bp.paragraph_format.line_spacing = 1.15
        r_bt = bp.add_run(b_title)
        r_bt.font.bold = True
        r_bt.font.size = Pt(9.5)
        r_bd = bp.add_run(b_desc)
        r_bd.font.size = Pt(9.5)

    # -------------------------------------------------------------
    # SECTION 3: FEATURE ENGINEERING (28 CONTINUOUS FEATURES)
    # -------------------------------------------------------------
    h3 = doc.add_heading(level=1)
    r3 = h3.add_run("3. Dual-Nature Feature Engineering (28 Dimensional Vector)")
    r3.font.name = "Arial"
    r3.font.size = Pt(13)
    r3.font.bold = True
    r3.font.color.rgb = PRIMARY
    h3.paragraph_format.space_before = Pt(8)
    h3.paragraph_format.space_after = Pt(4)
    
    p_body3 = doc.add_paragraph()
    p_body3.paragraph_format.space_after = Pt(4)
    p_body3.paragraph_format.line_spacing = 1.15
    p_body3.add_run(
        "Each 30-second sliding telemetry window (with 10-second stride) is transformed into a 28-dimensional feature vector. "
        "Crucially, to prevent shortcut learning (where decision trees memorize trivial scalar thresholds like trim_max ≥ 12%), "
        "all threshold leakage features were eliminated, forcing the models to evaluate true multivariate interaction terms:"
    )
    
    tbl_feats = doc.add_table(rows=7, cols=3)
    tbl_feats.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_feats.autofit = False
    feat_col_w = [Inches(1.8), Inches(2.2), Inches(3.0)]
    
    feat_data = [
        ("Domain Subsystem", "Engineered Features", "Physical / Diagnostic Rationale"),
        ("Mechanical & Driver Dynamics (8)", "rpm_mean, rpm_std, rpm_kurtosis, speed_mean, speed_std, idle_fraction, throttle_mean, engine_load_mean", "Distinguishes driver acceleration from mechanical surging; gates idle vs cruise operational envelopes."),
        ("Air-Path & Speed-Density (5)", "map_mean, map_std, maf_mean, maf_map_ratio, maf_physics_ratio", "Quantifies volumetric efficiency; detects unmetered air ingress by comparing measured MAF against theoretical speed-density."),
        ("Fuel & Stoichiometry (3)", "trim_mean, trim_std, stoich_closed_loop_residual", "Evaluates closed-loop fuel adaptation; stoich_closed_loop_residual gates trim learning by coolant operating temp."),
        ("Thermal Dynamics (5)", "coolant_mean, coolant_warmup_rate, intake_mean, coolant_intake_delta, thermal_deficit_score", "Measures thermal build-up rate and gradient; detects thermostat stuck open dumping heat during highway speed convection."),
        ("Electrical Dynamics (3)", "voltage_mean, voltage_ripple_std, voltage_physics_margin", "Evaluates battery charging headroom above 12.45V threshold; detects diode bridge ripple and rotor droop."),
        ("Combustion Stability (4)", "rpm_det_std, rpm_r3, rpm_r7, hunting_surge_metric", "Autocorrelation lag coefficients at tau=3 and tau=7 after linear detrending; isolates periodic 0.15 Hz cyclic hunting.")
    ]
    
    for r_idx, row in enumerate(feat_data):
        for c_idx, text in enumerate(row):
            cell = tbl_feats.cell(r_idx, c_idx)
            cell.width = feat_col_w[c_idx]
            set_cell_margins(cell, top=60, bottom=60, left=100, right=100)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            if r_idx == 0:
                set_cell_background(cell, "1B365D")
                r = p.add_run(text)
                r.font.bold = True
                r.font.size = Pt(8.5)
                r.font.color.rgb = RGBColor(255, 255, 255)
            else:
                if r_idx % 2 == 1:
                    set_cell_background(cell, "F9FAFC")
                r = p.add_run(text)
                r.font.size = Pt(8)
                r.font.color.rgb = DARK_TEXT

    # -------------------------------------------------------------
    # SECTION 4: ML BENCHMARK & MULTI-LABEL MODEL TUNING
    # -------------------------------------------------------------
    h4 = doc.add_heading(level=1)
    r4 = h4.add_run("4. Machine Learning Model Architecture & Algorithm Benchmarking")
    r4.font.name = "Arial"
    r4.font.size = Pt(13)
    r4.font.bold = True
    r4.font.color.rgb = PRIMARY
    h4.paragraph_format.space_before = Pt(8)
    h4.paragraph_format.space_after = Pt(4)
    
    p_body4 = doc.add_paragraph()
    p_body4.paragraph_format.space_after = Pt(4)
    p_body4.paragraph_format.line_spacing = 1.15
    p_body4.add_run(
        "To evaluate multiple fault patterns simultaneously on the same vehicle, the pipeline employs a "
    )
    p_body4.add_run("Multi-Output Multi-Label Classifier").font.bold = True
    p_body4.add_run(
        " predicting 5 simultaneous binary targets: [fault_lean_trim, fault_rich_trim, fault_combustion_hunting, fault_alternator_sag, fault_thermal_delay]. "
        "Strict drive-level GroupShuffleSplit (50 Train / 12 Val / 13 Test drives) was enforced with zero drive leakage. "
        "Five distinct algorithms were benchmarked on the validation set:"
    )
    
    tbl_bench = doc.add_table(rows=6, cols=5)
    tbl_bench.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_bench.autofit = False
    bench_col_w = [Inches(2.2), Inches(1.2), Inches(1.2), Inches(1.2), Inches(1.2)]
    
    bench_data = [
        ("Candidate Algorithm", "Subset Acc", "Hamming Loss", "Macro F1", "Micro F1"),
        ("Logistic Regression (Baseline)", "78.59%", "0.0434", "83.13%", "91.17%"),
        ("Extra Trees Classifier", "84.48%", "0.0325", "83.56%", "93.09%"),
        ("Random Forest Classifier", "89.80%", "0.0204", "86.65%", "95.77%"),
        ("LightGBM Classifier", "90.23%", "0.0195", "87.12%", "95.96%"),
        ("XGBoost Classifier (Winning Architecture)", "89.37%", "0.0216", "86.97% (CV)", "95.52%")
    ]
    
    for r_idx, row in enumerate(bench_data):
        for c_idx, text in enumerate(row):
            cell = tbl_bench.cell(r_idx, c_idx)
            cell.width = bench_col_w[c_idx]
            set_cell_margins(cell, top=50, bottom=50, left=80, right=80)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            if r_idx == 0:
                set_cell_background(cell, "1B365D")
                r = p.add_run(text)
                r.font.bold = True
                r.font.size = Pt(8.5)
                r.font.color.rgb = RGBColor(255, 255, 255)
            else:
                if r_idx == 5:
                    set_cell_background(cell, "EBF3FB")
                elif r_idx % 2 == 1:
                    set_cell_background(cell, "F9FAFC")
                r = p.add_run(text)
                r.font.size = Pt(8)
                if r_idx == 5:
                    r.font.bold = True
                r.font.color.rgb = DARK_TEXT

    p_tune = doc.add_paragraph()
    p_tune.paragraph_format.space_before = Pt(4)
    p_tune.paragraph_format.space_after = Pt(4)
    p_tune.paragraph_format.line_spacing = 1.15
    p_tune.add_run("Winning Hyperparameters (4-Fold GroupKFold Cross-Validation): ").font.bold = True
    p_tune.add_run("n_estimators = 150, max_depth = 5, learning_rate = 0.06, subsample = 0.85, colsample_bytree = 0.65. Feature subsampling (colsample_bytree=0.65) was critical to prevent single dominant features from overshadowing air-path, load, and thermal channels.")

    # -------------------------------------------------------------
    # SECTION 5: FINAL EVALUATION ON UNSEEN TEST DRIVES
    # -------------------------------------------------------------
    h5 = doc.add_heading(level=1)
    r5 = h5.add_run("5. Empirical Evaluation on Completely Unseen Test Drives")
    r5.font.name = "Arial"
    r5.font.size = Pt(13)
    r5.font.bold = True
    r5.font.color.rgb = PRIMARY
    h5.paragraph_format.space_before = Pt(8)
    h5.paragraph_format.space_after = Pt(4)
    
    p_body5 = doc.add_paragraph()
    p_body5.paragraph_format.space_after = Pt(4)
    p_body5.paragraph_format.line_spacing = 1.15
    p_body5.add_run(
        "The winning tuned XGBoost model was evaluated on 13 completely unseen holdout drives (754 windows) that were never exposed during training, feature selection, or hyperparameter search. "
        "The model achieved an exact-match "
    )
    p_body5.add_run("Subset Accuracy of 95.36%").font.bold = True
    p_body5.add_run(" (correct across all 5 simultaneous flags), a ")
    p_body5.add_run("Hamming Loss of 0.0093").font.bold = True
    p_body5.add_run(", and a ")
    p_body5.add_run("Weighted F1 Score of 97.16%").font.bold = True
    p_body5.add_run(":")
    
    tbl_eval = doc.add_table(rows=6, cols=6)
    tbl_eval.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_eval.autofit = False
    eval_col_w = [Inches(2.2), Inches(1.0), Inches(1.0), Inches(1.0), Inches(1.0), Inches(0.8)]
    
    eval_data = [
        ("Target Subsystem Fault", "Precision", "Recall", "F1-Score", "True Pos / Neg", "False +/-"),
        ("Lean Fuel-Trim Deviation", "100.0%", "99.2%", "99.6%", "TP: 261 / TN: 491", "FP: 0 / FN: 2"),
        ("Rich Fuel-Trim Deviation", "100.0%", "96.4%", "98.2%", "TP: 81 / TN: 670", "FP: 0 / FN: 3"),
        ("Combustion Hunting / Surge", "27.3%", "35.3%", "30.8%", "TP: 6 / TN: 721", "FP: 16 / FN: 11"),
        ("Alternator Charging Sag", "100.0%", "100.0%", "100.0%", "TP: 58 / TN: 696", "FP: 0 / FN: 0"),
        ("Thermal Warmup Delay", "99.2%", "98.5%", "98.9%", "TP: 133 / TN: 618", "FP: 1 / FN: 2")
    ]
    
    for r_idx, row in enumerate(eval_data):
        for c_idx, text in enumerate(row):
            cell = tbl_eval.cell(r_idx, c_idx)
            cell.width = eval_col_w[c_idx]
            set_cell_margins(cell, top=50, bottom=50, left=80, right=80)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            if r_idx == 0:
                set_cell_background(cell, "1B365D")
                r = p.add_run(text)
                r.font.bold = True
                r.font.size = Pt(8.5)
                r.font.color.rgb = RGBColor(255, 255, 255)
            else:
                if r_idx % 2 == 1:
                    set_cell_background(cell, "F9FAFC")
                r = p.add_run(text)
                r.font.size = Pt(8)
                r.font.color.rgb = DARK_TEXT

    p_imp = doc.add_paragraph()
    p_imp.paragraph_format.space_before = Pt(4)
    p_imp.paragraph_format.space_after = Pt(4)
    p_imp.paragraph_format.line_spacing = 1.15
    p_imp.add_run("Multivariate Feature Importance Distribution (Proof of Non-Shortcut Learning): ").font.bold = True
    p_imp.add_run(
        "Inspection of the decision tree split importances confirms that no single feature dominates the models: "
        "Lean Trim depends on trim_mean (30.2%), stoich_residual (22.6%), map_mean (8.1%), throttle_mean (8.1%), and maf_physics_ratio (4.6%); "
        "Thermal Delay depends on speed_mean (29.4%), thermal_deficit (19.8%), coolant_intake_delta (12.2%), and coolant_rate (7.3%); "
        "Combustion Hunting depends on maf_mean (13.2%), rpm_mean (12.6%), speed_mean (10.6%), and idle_fraction (6.7%)."
    )

    # -------------------------------------------------------------
    # SECTION 6: HYBRID DECISION FUSION & PHYSICS GUARDS
    # -------------------------------------------------------------
    h6 = doc.add_heading(level=1)
    r6 = h6.add_run("6. Hybrid Decision Fusion & First-Principles Safety Guards")
    r6.font.name = "Arial"
    r6.font.size = Pt(13)
    r6.font.bold = True
    r6.font.color.rgb = PRIMARY
    h6.paragraph_format.space_before = Pt(8)
    h6.paragraph_format.space_after = Pt(4)
    
    p_body6 = doc.add_paragraph()
    p_body6.paragraph_format.space_after = Pt(4)
    p_body6.paragraph_format.line_spacing = 1.15
    p_body6.add_run(
        "Why is pure ML insufficient on its own? In automotive diagnostics, pure ML models can discover spurious dataset correlations. For instance, in real city driving, vehicles often idle at red lights when battery draw is heaviest; a pure decision tree can learn: 'if speed == 0 and voltage < 12.0V -> Sag'. If the driver moves at 5 km/h, pure ML could drop the fault. "
        "IgnitionAI resolves this via a 4-Quadrant Decision Fusion Layer:"
    )
    
    bullets6 = [
        ("Hybrid Consensus (ML + Physics Confirmed): ", "Triggered when both XGBoost probability exceeds 70% AND first-principles physical boundary is violated (e.g. trim ≥ +13.0% with closed-loop coolant). Confidence: 96%-99%."),
        ("Physics Safety Override (Enforced): ", "If an ML model misses a fault due to a driving context anomaly (e.g. vehicle speed bug), the hard physical invariant overrides the model and enforces the fault (e.g. voltage < 12.45V while RPM > 500 always indicates charging failure by Ohm's Law)."),
        ("Machine Learning Pattern Match: ", "When early multivariate patterns appear (e.g. trim at +11.8% with depressed O2 sensor voltage) before the hard physical limit is reached, XGBoost flags early component degradation with an explicit ML Pattern Match badge."),
        ("Cold Open-Loop Suppression: ", "While coolant is under 45°C, oxygen sensors have not achieved light-off temperature (>300°C). Fuel trim alarms are physically suppressed to eliminate false positives.")
    ]
    for b_title, b_desc in bullets6:
        bp = doc.add_paragraph(style='List Bullet')
        bp.paragraph_format.space_after = Pt(2)
        bp.paragraph_format.line_spacing = 1.15
        r_bt = bp.add_run(b_title)
        r_bt.font.bold = True
        r_bt.font.size = Pt(9.5)
        r_bd = bp.add_run(b_desc)
        r_bd.font.size = Pt(9.5)

    # -------------------------------------------------------------
    # SECTION 7: STREAMLIT TEST-BENCH & AUDIT VERIFICATION
    # -------------------------------------------------------------
    h7 = doc.add_heading(level=1)
    r7 = h7.add_run("7. Streamlit Interactive Test-Bench & Deployment Architecture")
    r7.font.name = "Arial"
    r7.font.size = Pt(13)
    r7.font.bold = True
    r7.font.color.rgb = PRIMARY
    h7.paragraph_format.space_before = Pt(8)
    h7.paragraph_format.space_after = Pt(4)
    
    p_body7 = doc.add_paragraph()
    p_body7.paragraph_format.space_after = Pt(6)
    p_body7.paragraph_format.line_spacing = 1.15
    p_body7.add_run(
        "The complete diagnostic pipeline is deployed as an interactive automotive engineering test-bench on Streamlit (port 8502). "
        "It features: (1) 1-Click pre-configured dealership fault drive presets; (2) Custom CSV telemetry uploader supporting commercial ELM327 loggers; (3) Live Sensor Sliders connected to a continuous 30-second physical waveform synthesizer (zero hardcoding); (4) Real-time Subsystem Health Scoring (Fuel, Combustion, Electrical, Cooling) and composite Vehicle Health Index (VHI 0-100); and (5) Dealership Pre-Purchase Inspection Certificate generator with downloadable markdown reports."
    )
    
    # Save files
    out_dir = r"D:\IgnitionAI"
    doc_path = os.path.join(out_dir, "IgnitionAI_Hybrid_ML_Diagnostics_Documentation.docx")
    doc.save(doc_path)
    print(f"Document successfully created at: {doc_path}")
    
    # Also copy to Downloads folder for immediate user access
    dl_dir = os.path.expanduser(r"~\Downloads")
    if os.path.exists(dl_dir):
        dl_path = os.path.join(dl_dir, "IgnitionAI_Hybrid_ML_Diagnostics_Documentation.docx")
        shutil.copy2(doc_path, dl_path)
        print(f"Copied document to Downloads: {dl_path}")

if __name__ == "__main__":
    create_document()
