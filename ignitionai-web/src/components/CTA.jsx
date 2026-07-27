import React from 'react';
import { motion } from 'framer-motion';
import './CTA.css';

const mailPilot   = "mailto:founder@ignitionai.in?subject=Request%20for%20Early%20Access%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI%20came%20across%20IgnitionAI%20and%20I%27m%20very%20interested%20in%20the%20early%20access%20pilot.%0A%0AHere%27s%20a%20bit%20about%20our%20workshop%3A%0A%0AWorkshop%20Name%3A%20%0ALocation%3A%20%0ANumber%20of%20Bays%3A%20%0A%0APlease%20let%20me%20know%20the%20next%20steps.%0A%0ARegards%2C";
const mailDemo    = "mailto:founder@ignitionai.in?subject=Demo%20Request%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI%27d%20love%20to%20see%20a%20live%20demo%20of%20the%20IgnitionAI%20diagnostic%20platform.%0A%0AAvailability%3A%20%0APreferred%20mode%20(call%2Fin-person)%3A%20%0A%0ARegards%2C";
const mailPricing = "mailto:founder@ignitionai.in?subject=Pricing%20Enquiry%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI%27m%20interested%20in%20learning%20more%20about%20IgnitionAI%20pricing%20for%20my%20workshop.%0A%0AWorkshop%20Name%3A%0ACity%3A%0ANumber%20of%20Bays%3A%0APhone%2FWhatsApp%3A%0A%0ALooking%20forward%20to%20hearing%20from%20you.";

export default function CTA() {
  return (
    <section className="cta-section">
      <div className="container">
        <motion.div
          className="cta-card glass"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.7, ease: [0.25, 0.46, 0.45, 0.94] }}
        >
          <div className="cta-glow" />
          <div className="cta-inner">
            <div className="section-label" style={{ margin: '0 auto 1.5rem' }}>Limited Pilot</div>
            <h2 className="cta-title">Be Among the First Workshops<br />to Use IgnitionAI</h2>
            <p className="cta-desc">
              We are currently onboarding a select group of pilot workshops across India. Early partners get direct input on the product roadmap and preferred terms.
            </p>
            <div className="cta-actions">
              <a href={mailPilot} className="btn btn-primary btn-lg">
                Request Early Access
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              </a>
              <a href={mailDemo} className="btn btn-secondary btn-lg">
                Request a Demo
              </a>
              <a href={mailPricing} className="btn btn-ghost btn-lg" style={{ fontSize: '0.9rem' }}>
                Enquire Pricing
              </a>
            </div>
            <p className="cta-note">No commitment required. Just a conversation.</p>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
