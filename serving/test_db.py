import os
import sys

# Add the project root to the python path so 'serving' module can be found
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from serving.core.models import Base, FaultScenario, Symptom, DecisionNode, RootCause, RepairStep

# Use in-memory SQLite for testing
engine = create_engine("sqlite:///:memory:", echo=False)
Base.metadata.create_all(bind=engine)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def seed_and_test():
    db = SessionLocal()
    
    try:
        # Create a fault scenario
        scenario = FaultScenario(name="Random Misfire Engine", description="Multiple cylinder misfires with poor ignition")
        db.add(scenario)
        db.commit()
        db.refresh(scenario)

        # Create a symptom
        symptom = Symptom(scenario_id=scenario.id, phrasing="Engine running rough / vibrating", category="drivability")
        db.add(symptom)
        db.commit()
        db.refresh(symptom)

        # Create a decision node
        node = DecisionNode(symptom_id=symptom.id, question="Is the short term fuel trim > 10%?", condition="short_term_fuel_trim_b1 > 10")
        db.add(node)
        db.commit()
        db.refresh(node)

        # Create a root cause
        cause = RootCause(decision_node_id=node.id, description="Vacuum leak at intake manifold", probability=0.85)
        db.add(cause)
        db.commit()
        db.refresh(cause)

        # Create repair steps
        step1 = RepairStep(root_cause_id=cause.id, step_number=1, instruction="Perform smoke test on intake manifold")
        step2 = RepairStep(root_cause_id=cause.id, step_number=2, instruction="Replace intake manifold gasket if leak detected")
        db.add_all([step1, step2])
        db.commit()

        # Query and verify
        queried_scenario = db.query(FaultScenario).first()
        print(f"Scenario: {queried_scenario.name}")
        for sym in queried_scenario.symptoms:
            print(f"  Symptom: {sym.phrasing}")
            for n in sym.decision_nodes:
                print(f"    Decision: {n.question}")
                for rc in n.root_causes:
                    print(f"      Root Cause: {rc.description} ({rc.probability * 100}%)")
                    for rs in rc.repair_steps:
                        print(f"        Step {rs.step_number}: {rs.instruction}")
                        
        print("\nTest passed successfully!")

    except Exception as e:
        print(f"Test failed: {e}")
    finally:
        db.close()

if __name__ == "__main__":
    seed_and_test()
