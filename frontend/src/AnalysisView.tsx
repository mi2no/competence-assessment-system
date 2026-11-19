import { useEffect, useState } from "react";

type Analysis = {
    id : number,
	description : string,
	creationDate : Date
}

async function getAnalysis(artifactId : number) {
    const response = await fetch(`/api/artifact/${artifactId}/analysis`);

    if (!response.ok) return null;

    return await response.json().then(a => ({
        ...a,
        creationDate : new Date(a.creationDate)
    } as Analysis))
}

async function getState(artifactId : number) {
    const response = await fetch(`/api/artifact/${artifactId}/state`);

    if (!response.ok) return null;

    const state : string = await response.json();
    return state;
}

export default function AnalysisView({ artifactId } : { artifactId : number }) {
    const [state, setState] = useState<string | null>(null);
    const [analysis, setAnalysis] = useState<Analysis | null>(null);

    useEffect(() => {
        getState(artifactId).then(s => setState(s));
    }, [artifactId])

    useEffect(() => {
        if (state === "DONE")
            getAnalysis(artifactId).then(a => {
                console.log(a);
                setAnalysis(a);
    });
    }, [state]);

    return (
        <>
            <div className="sectionName">
                <h2>Analysis</h2>
                {state !== null && 
                    <>
                        &middot;
                        {
                            analysis === null ? <span>{state}</span> : <span>done on {analysis.creationDate.toLocaleString()}</span>
                        }
                    </>
                }
                </div>
            {analysis !== null && <div className="analysisDescription">{analysis.description}</div>}
        </>
    )
}