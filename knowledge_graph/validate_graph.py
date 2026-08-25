import yaml
import sys
import glob
from pathlib import Path
from pydantic import ValidationError
from schema import SubsystemGraph

# Force UTF-8 encoding for stdout on Windows
sys.stdout.reconfigure(encoding='utf-8')

def validate_all_subsystems(directory: str):
    yaml_files = glob.glob(f"{directory}/*.yaml")
    
    if not yaml_files:
        print(f"No YAML files found in {directory}")
        sys.exit(1)
        
    all_valid = True
    
    for file_path in yaml_files:
        print(f"Validating {file_path}...")
        try:
            with open(file_path, 'r') as f:
                data = yaml.safe_load(f)
            
            # Pydantic validation
            graph = SubsystemGraph(**data)
            print(f"✅ Success: '{graph.subsystem_name}' subsystem graph is strictly valid.")
            print(f"   - Components mapped: {len(graph.components)}")
            for comp in graph.components:
                print(f"     * {comp.name} ({len(comp.fault_modes)} fault modes)")
            
        except ValidationError as e:
            print(f"❌ Validation Error in {file_path}:")
            print(e)
            all_valid = False
        except Exception as e:
            print(f"❌ Error reading {file_path}: {e}")
            all_valid = False
            
    if not all_valid:
        sys.exit(1)

if __name__ == "__main__":
    # Point to the subsystems directory relative to this script
    subsystems_dir = Path(__file__).parent / "subsystems"
    validate_all_subsystems(str(subsystems_dir))
