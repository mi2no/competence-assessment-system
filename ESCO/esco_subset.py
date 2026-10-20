import pandas as pd

def load(name, lang) -> pd.DataFrame:
    dir = "ESCO dataset - v1.2.1 - classification - " + lang + " - csv"
    file = name + "_" + lang + ".csv"
    return pd.read_csv(dir + "/" + file, dtype = {"code" : str})


def get_ISCO_groups_subset() -> pd.DataFrame:
    ISCO_groups = load("ISCOGroups", "en")
    return ISCO_groups[ISCO_groups["code"].str.startswith("25")]

def get_occupations_subset(ISCO_groups_subset : pd.DataFrame) -> pd.DataFrame:
    occupations = load("occupations", "en")
    ISCO_code_set = frozenset(ISCO_groups_subset["code"].unique())
    return occupations[occupations["iscoGroup"].astype(str).isin(ISCO_code_set)]

def get_occupation_skill_relations_subset(occupations_subset : pd.DataFrame) -> pd.DataFrame:
    occupation_skill_relations = load("occupationSkillRelations", "en")
    occupations_uri_set = frozenset(occupations_subset["conceptUri"].unique())
    return occupation_skill_relations[
        occupation_skill_relations["occupationUri"].astype(str).isin(occupations_uri_set)
    ]

def get_skills_subset(skill_uri : frozenset) -> pd.DataFrame:    
    skills = load("skills", "en")
    return skills[skills["conceptUri"].astype(str).isin(skill_uri)]

output = "IT_subset"

ISCO_groups_subset = get_ISCO_groups_subset()
ISCO_groups_subset.to_csv(output + "/ISCOGroups.csv", index = False)

occupations_subset = get_occupations_subset(ISCO_groups_subset)
occupations_subset.to_csv(output + "/occupations.csv", index = False)

occupation_skill_relations_subset = get_occupation_skill_relations_subset(occupations_subset)
occupation_skill_relations_subset.to_csv(output + "/occupationSkillRelations.csv", index = False)

skills_subset = get_skills_subset(frozenset(occupation_skill_relations_subset["skillUri"].unique()))
skills_subset.to_csv(output + "/skills.csv", index = False)