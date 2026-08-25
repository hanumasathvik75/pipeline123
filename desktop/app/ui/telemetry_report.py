"""Monochrome PDF export for real telemetry diagnostic sessions."""
from __future__ import annotations

import html
import os
import re
from datetime import datetime

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import Flowable, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle


LOGO_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "logo.png")
BLACK = colors.HexColor("#111111")
INK = colors.HexColor("#252525")
MID = colors.HexColor("#666666")
LIGHT = colors.HexColor("#E6E6E6")
PAPER = colors.HexColor("#F7F7F7")


class CroppedLogo(Flowable):
    """Render the supplied logo asset with its empty canvas clipped away."""

    def __init__(self, width: float = 82 * mm, height: float = 25 * mm) -> None:
        super().__init__()
        self.width = width
        self.height = height

    def draw(self) -> None:
        if not os.path.exists(LOGO_PATH):
            return
        self.canv.saveState()
        clip = self.canv.beginPath()
        clip.rect(0, 0, self.width, self.height)
        self.canv.clipPath(clip, stroke=0, fill=0)
        # Source content bounds are cropped at render time; pixels are unchanged.
        scale = self.width / 2100.0
        self.canv.drawImage(LOGO_PATH, -350 * scale, -430 * scale, 2816 * scale, 1536 * scale, preserveAspectRatio=False, mask="auto")
        self.canv.restoreState()


def _safe(value: object) -> str:
    return html.escape(str(value))


def _plain_model_text(value: str) -> str:
    """Normalize model Markdown into clean PDF text instead of exposing markup."""
    text = re.sub(r"\*\*(.*?)\*\*", r"\1", value)
    text = text.replace("`", "")
    text = re.sub(r"^\s*[-*]\s+", "", text, flags=re.MULTILINE)
    return text.strip()


def generate_telemetry_pdf_report(path: str, result: dict, mechanic_report: str = "") -> None:
    """Export a diagnostic report from structured analysis output."""
    doc = SimpleDocTemplate(path, pagesize=A4, rightMargin=17 * mm, leftMargin=17 * mm, topMargin=16 * mm, bottomMargin=17 * mm, title="IgnitionAI Vehicle Diagnostic Report", author="IgnitionAI")
    styles = getSampleStyleSheet()
    body = ParagraphStyle("Body", parent=styles["BodyText"], fontName="Helvetica", fontSize=8.7, leading=12, textColor=INK)
    small = ParagraphStyle("Small", parent=body, fontSize=7.5, leading=10, textColor=MID)
    table_header = ParagraphStyle("TableHeader", parent=small, textColor=colors.white, fontName="Helvetica-Bold")
    section = ParagraphStyle("Section", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=10, leading=12, textColor=BLACK, spaceBefore=11, spaceAfter=7, uppercase=True)
    report_style = ParagraphStyle("Report", parent=body, fontSize=9, leading=13, leftIndent=8, rightIndent=8, borderColor=LIGHT, borderWidth=0.7, borderPadding=9, backColor=PAPER)

    source = result["source"]
    fusion = result["fusion"]
    generated = datetime.now().strftime("%d %b %Y, %H:%M")
    story: list = []
    header = Table([[CroppedLogo(), Paragraph(f"<b>VEHICLE DIAGNOSTIC REPORT</b><br/><font size='8' color='#666666'>Evidence-first telemetry analysis<br/>{generated}</font>", body)]], colWidths=[96 * mm, 80 * mm])
    header.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("ALIGN", (1, 0), (1, 0), "RIGHT"), ("LINEBELOW", (0, 0), (-1, -1), 1.2, BLACK), ("BOTTOMPADDING", (0, 0), (-1, -1), 8)]))
    story.extend([header, Spacer(1, 7 * mm)])

    summary = Table([
        [Paragraph("<b>VEHICLE HEALTH INDEX</b>", small), Paragraph("<b>ANALYZED SESSION</b>", small), Paragraph("<b>SIGNAL COVERAGE</b>", small)],
        [Paragraph(f"<font size='22'><b>{_safe(fusion.get('vehicle_health_index', 'N/A'))}</b></font> / 100", body), Paragraph(f"{_safe(source.get('selected_segment') or 'Single stream')}<br/><font size='8'>{source.get('rows', 0):,} samples</font>", body), Paragraph(f"{len(fusion.get('available_indices', []))} virtual sensors<br/><font size='8'>{_safe(source.get('sample_interval_seconds'))} s median cadence</font>", body)],
    ], colWidths=[57 * mm, 61 * mm, 58 * mm])
    summary.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), PAPER), ("BOX", (0, 0), (-1, -1), 0.8, LIGHT), ("INNERGRID", (0, 0), (-1, -1), 0.5, LIGHT), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("LEFTPADDING", (0, 0), (-1, -1), 9), ("RIGHTPADDING", (0, 0), (-1, -1), 9), ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7)]))
    story.append(summary)

    story.append(Paragraph("VIRTUAL SENSOR RESULTS", section))
    sensor_rows = [[Paragraph("Index", table_header), Paragraph("Health", table_header), Paragraph("State", table_header), Paragraph("Evidence", table_header)]]
    for sensor in result.get("virtual_sensors", []):
        evidence = "; ".join(f"{key.replace('_', ' ')}: {value}" for key, value in sensor.get("evidence", {}).items() if key != "flag_timestamps")
        health = "Insufficient data" if sensor.get("health_score") is None else f"{sensor['health_score']:.1f}%"
        state = "Flagged" if sensor.get("flagged") else sensor.get("status", "ok").replace("_", " ").title()
        sensor_rows.append([Paragraph(_safe(sensor["name"].replace("_", " ").title()), body), Paragraph(_safe(health), body), Paragraph(_safe(state), body), Paragraph(_safe(evidence), small)])
    sensor_table = Table(sensor_rows, repeatRows=1, colWidths=[36 * mm, 23 * mm, 24 * mm, 93 * mm])
    sensor_table.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), BLACK), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white), ("BOX", (0, 0), (-1, -1), 0.6, LIGHT), ("INNERGRID", (0, 1), (-1, -1), 0.4, LIGHT), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    story.append(sensor_table)

    story.append(Paragraph("RANKED FAULT CANDIDATES", section))
    fault_rows = [[Paragraph("Candidate", table_header), Paragraph("Confidence", table_header), Paragraph("Supporting evidence", table_header)]]
    faults = fusion.get("fault_candidates", [])
    if faults:
        for fault in faults:
            evidence = "; ".join(f"{key.replace('_', ' ')}: {value}" for key, value in fault.get("evidence", {}).items() if key != "flag_timestamps")
            fault_rows.append([Paragraph(_safe(fault["candidate"].replace("_", " ").title()), body), Paragraph(f"{fault['confidence'] * 100:.0f}%", body), Paragraph(_safe(evidence), small)])
    else:
        fault_rows.append([Paragraph("No configured evidence threshold crossed", body), Paragraph("N/A", body), Paragraph("The engine did not fabricate a fault candidate.", small)])
    fault_table = Table(fault_rows, repeatRows=1, colWidths=[50 * mm, 27 * mm, 99 * mm])
    fault_table.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), BLACK), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white), ("BOX", (0, 0), (-1, -1), 0.6, LIGHT), ("INNERGRID", (0, 1), (-1, -1), 0.4, LIGHT), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    story.append(fault_table)

    story.append(Paragraph("MECHANIC REPORT", section))
    narrative = _plain_model_text(mechanic_report) or "No narrative report was generated. The diagnostic results above remain the authoritative structured output."
    for paragraph in narrative.split("\n\n"):
        if paragraph.strip():
            story.append(Paragraph(_safe(paragraph.strip()).replace("\n", "<br/>"), report_style))
            story.append(Spacer(1, 2 * mm))

    plans = result.get("repair_plan", [])
    if plans:
        story.append(PageBreak())
        story.append(Paragraph("STEPWISE DIAGNOSTIC & REPAIR PLAN", section))
        story.append(Paragraph("Evidence-gated workshop workflow. Complete verification steps in order; replace or repair a component only after the preceding test identifies a failure.", body))
        story.append(Spacer(1, 4 * mm))
        for plan in plans:
            name = plan["candidate"].replace("_", " ").title()
            confidence = float(plan.get("confidence") or 0) * 100
            story.append(Paragraph(f"{plan['rank']}. {_safe(name)} <font color='#666666'>({confidence:.0f}% confidence)</font>", section))
            step_rows = [[Paragraph("Step", table_header), Paragraph("Workshop action", table_header), Paragraph("Method", table_header), Paragraph("Pass condition", table_header)]]
            for index, step in enumerate(plan.get("steps", []), start=1):
                step_rows.append([
                    Paragraph(f"{index}<br/><font size='7' color='#666666'>{_safe(step['stage'])}</font>", body),
                    Paragraph(_safe(step["action"]), body),
                    Paragraph(_safe(step["method"]), small),
                    Paragraph(_safe(step["pass"]), small),
                ])
            plan_table = Table(step_rows, repeatRows=1, colWidths=[18 * mm, 39 * mm, 72 * mm, 47 * mm])
            plan_table.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), BLACK), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white), ("BOX", (0, 0), (-1, -1), 0.6, LIGHT), ("INNERGRID", (0, 1), (-1, -1), 0.4, LIGHT), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
            story.append(plan_table)
            story.append(Spacer(1, 4 * mm))

    story.append(Spacer(1, 5 * mm))
    story.append(Paragraph("Method boundary: raw OBD telemetry was processed locally by deterministic physical and statistical algorithms. The narrative model received only the VHI and structured fault evidence. This report is decision support and requires confirmation by a qualified technician.", small))

    def footer(canvas, document) -> None:
        canvas.saveState(); canvas.setStrokeColor(LIGHT); canvas.line(17 * mm, 13 * mm, 193 * mm, 13 * mm); canvas.setFillColor(MID); canvas.setFont("Helvetica", 7); canvas.drawString(17 * mm, 8 * mm, "IgnitionAI - physics-informed vehicle diagnostics"); canvas.drawRightString(193 * mm, 8 * mm, f"Page {document.page}"); canvas.restoreState()

    doc.build(story, onFirstPage=footer, onLaterPages=footer)
