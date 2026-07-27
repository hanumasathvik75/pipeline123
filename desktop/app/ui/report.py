"""
IgnitionAI — PDF Report Generator
Generates a branded professional report using ReportLab.
"""

import os
from datetime import datetime
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    HRFlowable, Image
)
from reportlab.lib.colors import HexColor

LOGO_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "logo.png")

# ─── Color Palette ─────────────────────────────────────────────────────────
C_DARK_BLUE  = HexColor("#0A1628")
C_MID_BLUE   = HexColor("#1565C0")
C_LIGHT_BLUE = HexColor("#5B9BD5")
C_CYAN       = HexColor("#00D4FF")
C_TEXT       = HexColor("#1A2D5A")
C_MUTED      = HexColor("#4A6080")
C_SUCCESS    = HexColor("#00897B")
C_DANGER     = HexColor("#C62828")
C_WARNING    = HexColor("#F57F17")
C_WHITE      = colors.white
C_LIGHT_BG   = HexColor("#F0F6FF")
C_CARD_BG    = HexColor("#E8F0FC")


def generate_pdf_report(
    path: str,
    results: dict,
    diagnosis_text: str = "",
    repair_text: str = "",
):
    doc = SimpleDocTemplate(
        path,
        pagesize=A4,
        rightMargin=2 * cm,
        leftMargin=2 * cm,
        topMargin=2 * cm,
        bottomMargin=2 * cm,
    )

    styles = getSampleStyleSheet()

    # Custom styles
    title_style = ParagraphStyle(
        "Title",
        fontSize=22,
        textColor=C_DARK_BLUE,
        fontName="Helvetica-Bold",
        spaceAfter=6,
        alignment=TA_CENTER,
    )
    subtitle_style = ParagraphStyle(
        "Subtitle",
        fontSize=11,
        textColor=C_MUTED,
        fontName="Helvetica",
        spaceAfter=4,
        alignment=TA_CENTER,
    )
    section_style = ParagraphStyle(
        "Section",
        fontSize=12,
        textColor=C_WHITE,
        fontName="Helvetica-Bold",
        spaceBefore=14,
        spaceAfter=8,
        leftIndent=8,
    )
    body_style = ParagraphStyle(
        "Body",
        fontSize=10,
        textColor=C_TEXT,
        fontName="Helvetica",
        spaceAfter=6,
        leading=15,
    )
    bold_body = ParagraphStyle(
        "BoldBody",
        fontSize=10,
        textColor=C_TEXT,
        fontName="Helvetica-Bold",
        spaceAfter=4,
    )
    small_style = ParagraphStyle(
        "Small",
        fontSize=8,
        textColor=C_MUTED,
        fontName="Helvetica",
        spaceAfter=4,
    )

    story = []

    # ── Header ────────────────────────────────────────────────────────────
    # Logo + title in a table
    logo_cell = ""
    if os.path.exists(LOGO_PATH):
        try:
            logo_img = Image(LOGO_PATH, width=5 * cm, height=1.5 * cm)
            logo_cell = logo_img
        except Exception:
            logo_cell = Paragraph("IgnitionAI", title_style)
    else:
        logo_cell = Paragraph("IgnitionAI", title_style)

    now = datetime.now().strftime("%Y-%m-%d %H:%M")
    header_data = [
        [logo_cell, Paragraph(f"<b>VEHICLE DIAGNOSTIC REPORT</b><br/><font size='9' color='grey'>Generated: {now}</font>", subtitle_style)],
    ]
    header_table = Table(header_data, colWidths=[8 * cm, 9.5 * cm])
    header_table.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (1, 0), (1, 0), "RIGHT"),
        ("LINEBELOW", (0, 0), (-1, 0), 1.5, C_MID_BLUE),
        ("BOTTOMPADDING", (0, 0), (-1, 0), 12),
    ]))
    story.append(header_table)
    story.append(Spacer(1, 0.4 * cm))

    # ── Executive Summary ─────────────────────────────────────────────────
    final_root = results.get("final_root", "Unknown")
    final_decision = results.get("final_decision", {})
    confidence = final_decision.get("confidence", 0)
    method = final_decision.get("method", "heuristic")
    dtcs = results.get("input_dtcs", [])
    confidence_pct = f"{confidence * 100:.0f}%"

    sev_color = C_DANGER if confidence >= 0.8 else C_WARNING if confidence >= 0.5 else C_SUCCESS

    summary_data = [
        [
            Paragraph(f"<b>Root Cause Identified</b><br/><font size='13'>{final_root}</font>", body_style),
            Paragraph(f"<b>AI Confidence</b><br/><font size='18' color='{sev_color.hexval()}'><b>{confidence_pct}</b></font>", body_style),
            Paragraph(f"<b>Method</b><br/>{method.title()}", body_style),
            Paragraph(f"<b>Fault Codes</b><br/>{', '.join(dtcs) if dtcs else 'None'}", body_style),
        ]
    ]
    summary_table = Table(summary_data, colWidths=[5.5 * cm, 4 * cm, 3.5 * cm, 4.5 * cm])
    summary_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), C_CARD_BG),
        ("LINEABOVE", (0, 0), (-1, 0), 3, C_MID_BLUE),
        ("LINEBELOW", (0, 0), (-1, 0), 0.5, C_LIGHT_BLUE),
        ("INNERGRID", (0, 0), (-1, -1), 0.3, C_LIGHT_BLUE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 10),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 10),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("RIGHTPADDING", (0, 0), (-1, -1), 10),
        ("ROUNDEDCORNERS", [5]),
    ]))
    story.append(summary_table)
    story.append(Spacer(1, 0.4 * cm))

    # ── AI Diagnosis Section ──────────────────────────────────────────────
    if diagnosis_text:
        diag_header = Table(
            [[Paragraph("🤖  AI DIAGNOSIS — PLAIN ENGLISH", section_style)]],
            colWidths=[17.5 * cm],
        )
        diag_header.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), C_MID_BLUE),
            ("TOPPADDING", (0, 0), (-1, -1), 8),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ("LEFTPADDING", (0, 0), (-1, -1), 12),
        ]))
        story.append(diag_header)
        story.append(Spacer(1, 0.2 * cm))

        for para in diagnosis_text.split("\n\n"):
            if para.strip():
                story.append(Paragraph(para.strip(), body_style))
        story.append(Spacer(1, 0.3 * cm))

    # ── DTC Rankings Table ────────────────────────────────────────────────
    dtc_rankings = results.get("dtc_rankings", [])
    if dtc_rankings:
        dtc_header = Table(
            [[Paragraph("📊  RANKED FAULT CODES", section_style)]],
            colWidths=[17.5 * cm],
        )
        dtc_header.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), C_DARK_BLUE),
            ("TOPPADDING", (0, 0), (-1, -1), 8),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ("LEFTPADDING", (0, 0), (-1, -1), 12),
        ]))
        story.append(dtc_header)
        story.append(Spacer(1, 0.2 * cm))

        table_data = [[
            Paragraph("<b>Code</b>", bold_body),
            Paragraph("<b>Description</b>", bold_body),
            Paragraph("<b>Score</b>", bold_body),
            Paragraph("<b>Confidence</b>", bold_body),
        ]]
        for row in dtc_rankings[:10]:
            desc = row.get("description", "")[:60]
            plaus_pct = f"{row.get('avg_plaus', 0)*100:.0f}%"
            table_data.append([
                Paragraph(f"<b>{row['dtc']}</b>", body_style),
                Paragraph(desc, small_style),
                Paragraph(f"{row['score']:.2f}", body_style),
                Paragraph(plaus_pct, body_style),
            ])

        dtc_table = Table(table_data, colWidths=[2.5 * cm, 9 * cm, 2.5 * cm, 3.5 * cm])
        dtc_table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), C_LIGHT_BG),
            ("LINEBELOW", (0, 0), (-1, 0), 1, C_MID_BLUE),
            ("INNERGRID", (0, 1), (-1, -1), 0.25, HexColor("#DDEEFF")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 6),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ("LEFTPADDING", (0, 0), (-1, -1), 8),
            ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ]))
        story.append(dtc_table)
        story.append(Spacer(1, 0.4 * cm))

    # ── Root Cause Rankings ────────────────────────────────────────────────
    root_rankA = results.get("root_rankA", [])
    if root_rankA:
        rc_header = Table(
            [[Paragraph("🔍  ROOT CAUSE ANALYSIS", section_style)]],
            colWidths=[17.5 * cm],
        )
        rc_header.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), C_MID_BLUE),
            ("TOPPADDING", (0, 0), (-1, -1), 8),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ("LEFTPADDING", (0, 0), (-1, -1), 12),
        ]))
        story.append(rc_header)
        story.append(Spacer(1, 0.2 * cm))

        rc_data = [[Paragraph("<b>Root Cause</b>", bold_body), Paragraph("<b>Score</b>", bold_body)]]
        for i, rc in enumerate(root_rankA[:6]):
            prefix = "#1 ✓" if i == 0 else f"#{i+1}"
            rc_data.append([
                Paragraph(f"{prefix}  {rc.get('root', '')}", body_style),
                Paragraph(f"{rc.get('score', 0):.2f}", body_style),
            ])

        rc_table = Table(rc_data, colWidths=[14 * cm, 3.5 * cm])
        rc_table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), C_LIGHT_BG),
            ("BACKGROUND", (0, 1), (-1, 1), HexColor("#E8F5E9")),
            ("LINEBELOW", (0, 0), (-1, 0), 1, C_MID_BLUE),
            ("INNERGRID", (0, 1), (-1, -1), 0.25, HexColor("#DDEEFF")),
            ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ("TOPPADDING", (0, 0), (-1, -1), 6),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ]))
        story.append(rc_table)
        story.append(Spacer(1, 0.4 * cm))

    # ── Repair Steps ──────────────────────────────────────────────────────
    repair_steps = results.get("repair_steps", [])
    if repair_steps:
        rs_header = Table(
            [[Paragraph("🔧  RECOMMENDED REPAIR STEPS", section_style)]],
            colWidths=[17.5 * cm],
        )
        rs_header.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), C_DARK_BLUE),
            ("TOPPADDING", (0, 0), (-1, -1), 8),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ("LEFTPADDING", (0, 0), (-1, -1), 12),
        ]))
        story.append(rs_header)
        story.append(Spacer(1, 0.2 * cm))

        for i, step in enumerate(repair_steps, 1):
            story.append(Paragraph(f"<b>Step {i}:</b> {step}", body_style))
        story.append(Spacer(1, 0.3 * cm))

    # ── AI Repair Narrative ────────────────────────────────────────────────
    if repair_text:
        rn_header = Table(
            [[Paragraph("📋  AI REPAIR NARRATIVE", section_style)]],
            colWidths=[17.5 * cm],
        )
        rn_header.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), C_MID_BLUE),
            ("TOPPADDING", (0, 0), (-1, -1), 8),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ("LEFTPADDING", (0, 0), (-1, -1), 12),
        ]))
        story.append(rn_header)
        story.append(Spacer(1, 0.2 * cm))
        for para in repair_text.split("\n\n"):
            if para.strip():
                story.append(Paragraph(para.strip(), body_style))
        story.append(Spacer(1, 0.3 * cm))

    # ── Footer Disclaimer ──────────────────────────────────────────────────
    story.append(HRFlowable(width="100%", thickness=0.5, color=C_LIGHT_BLUE))
    story.append(Spacer(1, 0.2 * cm))
    footer_text = (
        "This report was generated by IgnitionAI using AI-assisted analysis. "
        "Results are diagnostic suggestions and should be validated by a certified automotive technician. "
        "IgnitionAI is not responsible for repair outcomes based on this report."
    )
    story.append(Paragraph(footer_text, small_style))

    doc.build(story)
