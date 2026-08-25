import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from serving.core.database import Base
from serving.core.models import FaultScenario, Symptom, DecisionNode, RootCause, RepairStep

# In-memory SQLite database for testing
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

@pytest.fixture(scope="module")
def db():
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()
        Base.metadata.drop_all(bind=engine)

def test_graph_logic_relationships(db):
    # Create the entities
    scenario = FaultScenario(name="Engine Misfire", description="Engine misfires randomly")
    symptom = Symptom(phrasing="Car shakes when accelerating", category="drivability", scenario=scenario)
    node = DecisionNode(question="Is fuel pressure < 20 psi?", condition="fuel_pressure < 20", symptom=symptom)
    cause = RootCause(description="Faulty fuel pump", probability=0.85, decision_node=node)
    step = RepairStep(step_number=1, instruction="Replace fuel pump relay", root_cause=cause)

    # Add to DB
    db.add(scenario)
    db.commit()

    # Query back and verify relationships
    db_scenario = db.query(FaultScenario).filter_by(name="Engine Misfire").first()
    assert db_scenario is not None
    assert len(db_scenario.symptoms) == 1
    
    db_symptom = db_scenario.symptoms[0]
    assert db_symptom.phrasing == "Car shakes when accelerating"
    assert len(db_symptom.decision_nodes) == 1
    
    db_node = db_symptom.decision_nodes[0]
    assert db_node.question == "Is fuel pressure < 20 psi?"
    assert len(db_node.root_causes) == 1
    
    db_cause = db_node.root_causes[0]
    assert db_cause.description == "Faulty fuel pump"
    assert len(db_cause.repair_steps) == 1
    
    db_step = db_cause.repair_steps[0]
    assert db_step.instruction == "Replace fuel pump relay"
