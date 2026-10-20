import pandas as pd

def load(name, lang) -> pd.DataFrame:
    dir = "ESCO dataset - v1.2.1 - classification - " + lang + " - csv"
    file = name + "_" + lang + ".csv"
    
    df = pd.read_csv(dir + "/" + file, dtype = {"code" : str})

    if "modifiedDate" in df.columns and "conceptUri" in df.columns:
        df["modifiedDate"] = pd.to_datetime(df["modifiedDate"], format = "ISO8601")
        df = df.sort_values("modifiedDate").drop_duplicates(subset = "conceptUri", keep = "last")

    return df

output = "IT_subset"

def save(frame : pd.DataFrame, name):
    print(name, frame.shape[0])
    frame.to_csv(output + "/" + name + ".csv", index = False)


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

def get_skill_skill_relations_subset(skill_uri : frozenset) -> pd.DataFrame:
    relations = load("skillSkillRelations", "en")
    return relations[
            relations["originalSkillUri"].astype(str).isin(skill_uri) &
            relations["relatedSkillUri"].astype(str).isin(skill_uri)
    ]

def get_broader_relations_skill_pillar(skill_uri : frozenset):
    out, stack = set(skill_uri), [*skill_uri]

    pillar = load("broaderRelationsSkillPillar", "en")
    parent = pillar.groupby("conceptUri")["broaderUri"].apply(list).to_dict()

    while stack:
        for p in parent.get(stack.pop(), []):
            if p not in out:
                out.add(p)
                stack.append(p)

    return (pillar[
            pillar["conceptUri"].astype(str).isin(out) & pillar["broaderUri"].astype(str).isin(out)
    ], out)

def get_skills_hierarchy(skill_uri : frozenset) -> pd.DataFrame:
    hierarchy = load("skillsHierarchy", "en")
    return hierarchy[hierarchy["Level 0 URI"].astype(str).isin(skill_uri)]

ISCO_groups_subset = get_ISCO_groups_subset()
save(ISCO_groups_subset, "ISCOGroups")

occupations_subset = get_occupations_subset(ISCO_groups_subset)
save(occupations_subset, "occupations")

occupation_skill_relations_subset = get_occupation_skill_relations_subset(occupations_subset)
save(occupation_skill_relations_subset, "occupationSkillRelations")

skill_uris = frozenset(occupation_skill_relations_subset["skillUri"].unique())

broader_relations_skill_pillar, skill_uris = get_broader_relations_skill_pillar(skill_uris)
save(broader_relations_skill_pillar, "broaderRelationsSkillPillar")

skills_subset = get_skills_subset(skill_uris)
save(skills_subset, "skills")

skill_skill_relations_subset = get_skill_skill_relations_subset(skill_uris)
save(skill_skill_relations_subset, "skillSkillRelations")

skills_hierarchy = get_skills_hierarchy(skill_uris)
save(skills_hierarchy, "skillsHierarchy")