import pandas as pd
from collections import Counter

D = "esco/"  # folder z rozpakowanymi CSV
skills   = pd.read_csv(D + "skills_en.csv")
groups   = pd.read_csv(D + "skillGroups_en.csv")
broader  = pd.read_csv(D + "broaderRelationsSkillPillar_en.csv")
occ      = pd.read_csv(D + "occupations_en.csv", dtype={"iscoGroup": str})
occ_sk   = pd.read_csv(D + "occupationSkillRelations_en.csv")
sk_sk    = pd.read_csv(D + "skillSkillRelations_en.csv")
skills_pl = pd.read_csv(D + "skills_pl.csv")[["conceptUri", "preferredLabel"]]
groups_pl = pd.read_csv(D + "skillGroups_pl.csv")[["conceptUri", "preferredLabel"]]

parent = broader.groupby("conceptUri")["broaderUri"].apply(list).to_dict()
children = broader.groupby("broaderUri")["conceptUri"].apply(list).to_dict()

def descendants(root):
    out, stack = set(), [root]
    while stack:
        for c in children.get(stack.pop(), []):
            if c not in out:
                out.add(c); stack.append(c)
    return out

def ancestors(uri):
    out, stack = set(), [uri]
    while stack:
        for p in parent.get(stack.pop(), []):
            if p not in out:
                out.add(p); stack.append(p)
    return out

# --- A: poddrzewa hierarchii (kody grup – sprawdź w skillGroups_en.csv, kolumna "code")
ROOT_CODES = ["06", "S5"]
roots = groups[groups["code"].astype(str).isin(ROOT_CODES)]["conceptUri"]
from_tree = set().union(*(descendants(r) for r in roots))

# --- B: kompetencje essential zawodów ICT
ict_occ = occ[occ["iscoGroup"].str.match(r"^(25|35|1330)")]["conceptUri"]
rel = occ_sk[occ_sk["occupationUri"].isin(ict_occ) & (occ_sk["relationType"] == "essential")]
freq = Counter(rel["skillUri"])

# --- C: przycięcie
MIN_OCC = 3
skill_info = skills.set_index("conceptUri")
selected = {u for u, n in freq.items() if n >= MIN_OCC}
selected |= {u for u in from_tree if u in skill_info.index and freq.get(u, 0) >= 1}
selected = {u for u in selected if skill_info.loc[u, "reuseLevel"] != "transversal"}

# domknięcie w górę
nodes = set(selected)
for u in selected:
    nodes |= ancestors(u)

# --- eksport
labels_en = pd.concat([skills, groups])[["conceptUri", "preferredLabel", "conceptType"]]
labels_pl = pd.concat([skills_pl, groups_pl]).rename(columns={"preferredLabel": "label_pl"})
nodes_df = (labels_en[labels_en["conceptUri"].isin(nodes)]
            .merge(labels_pl, on="conceptUri", how="left")
            .merge(skills[["conceptUri", "skillType", "description"]], on="conceptUri", how="left"))
nodes_df["ict_occupations"] = nodes_df["conceptUri"].map(freq).fillna(0).astype(int)

e1 = broader[broader["conceptUri"].isin(nodes) & broader["broaderUri"].isin(nodes)]
e1 = e1.rename(columns={"conceptUri": "source", "broaderUri": "target"})[["source", "target"]]
e1["relation"] = "isPartOf"

e2 = sk_sk[sk_sk["originalSkillUri"].isin(nodes) & sk_sk["relatedSkillUri"].isin(nodes)]
e2 = e2.rename(columns={"originalSkillUri": "source", "relatedSkillUri": "target",
                        "relationType": "relation"})[["source", "target", "relation"]]

nodes_df.to_csv("nodes.csv", index=False)
pd.concat([e1, e2]).to_csv("edges.csv", index=False)
print(len(nodes_df), "węzłów,", len(e1) + len(e2), "krawędzi")
