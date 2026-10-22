#!/usr/bin/env python3
"""Validate canonical Malefic Veil economy CSV references without dependencies."""
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "economy"

def read(name, key):
    with (ROOT / name).open(newline="", encoding="utf-8") as file:
        rows = list(csv.DictReader(file))
    ids = [row[key] for row in rows]
    assert len(ids) == len(set(ids)) if name not in {
        "industrial_recipe_inputs.csv", "facility_costs.csv", "hand_recipe_inputs.csv"
    } else True, f"Duplicate {key} in {name}"
    return rows

items = read("items.csv", "item_id")
facilities = read("facilities.csv", "facility_id")
industrial = read("industrial_recipes.csv", "recipe_id")
hand = read("hand_recipes.csv", "recipe_id")
recipe_inputs = read("industrial_recipe_inputs.csv", "recipe_id")
hand_inputs = read("hand_recipe_inputs.csv", "recipe_id")
facility_costs = read("facility_costs.csv", "facility_id")
storage = read("storage_containers.csv", "container_id")
skills = read("skill_modifiers.csv", "skill")
item_ids = {x["item_id"] for x in items}
facility_ids = {x["facility_id"] for x in facilities}
industrial_ids = {x["recipe_id"] for x in industrial}
hand_ids = {x["recipe_id"] for x in hand}
for name, rows, key, allowed in [
    ("industrial_recipe_inputs", recipe_inputs, "recipe_id", industrial_ids),
    ("hand_recipe_inputs", hand_inputs, "recipe_id", hand_ids),
    ("facility_costs", facility_costs, "facility_id", facility_ids)
]:
    for row in rows:
        assert row[key] in allowed, f"Unknown reference in {name}: {row}"
        assert row["item_id"] in item_ids, f"Unknown item in {name}: {row}"
        assert int(row["quantity"]) > 0, f"Nonpositive cost: {row}"
for recipe in industrial:
    assert recipe["station_id"] in facility_ids, recipe
    assert recipe["output_item"] in item_ids, recipe
    assert int(recipe["output_quantity"]) > 0
    assert int(recipe["seconds"]) > 0
for recipe in hand:
    assert recipe["output_item"] in item_ids, recipe
    assert int(recipe["output_quantity"]) > 0
for fac in facilities:
    for dep in filter(None, fac["prerequisites"].split(";")):
        assert dep in facility_ids, f"Unknown prerequisite: {dep}"
for item in items:
    assert int(item["unit_storage"]) > 0
    assert int(item["max_stack"]) > 0
for entry in storage:
    assert int(entry["capacity_units"]) > 0
    assert int(entry["max_slots"]) > 0
for effect in skills:
    assert 0 <= float(effect["per_level"]) <= 1
    assert 0 <= float(effect["max_bonus"]) <= 1
unmapped = [x["item_id"] for x in items if not x["icon_entry"]]
print(f"PASS economy references: {len(items)} items, {len(facilities)} facilities, "
      f"{len(industrial)} industrial recipes, {len(hand)} field recipes, "
      f"{len(storage)} storage classes, {len(skills)} skill effects")
print("Icon bindings awaiting artwork:", ", ".join(unmapped) or "none")
