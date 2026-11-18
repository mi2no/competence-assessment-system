import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import ArtifactView from "./ArtifactView.tsx";

import './TaskPage.css'

export type Task = {
    id : number,
    title : string,
    description : string,
    creationDate : Date,
    dueDate : Date,
    teacherId : number,
    courseId : number
}

type Artifact = {
    id : number,
    date : Date,
    description : string,
    uuid : string,
    userId : number
}

async function getTask(id : number) {
    const response = await fetch(`/api/task/get/${id}`);

    return await response.json().then(t => ({
        ...t,
        creationDate : new Date(t.creationDate),
        dueDate : new Date(t.dueDate)
    } as Task));
}

async function getSolution(taskId : number) {
    const response = await fetch(`/api/task/solution/${taskId}`);

    if (!response.ok) return null;

    return await response.json().then(a => ({
        ...a,
        date : new Date(a.date),
    } as Artifact));
}

async function submitSolution(form : HTMLFormElement, taskId : number) {
    const data = new FormData(form);

    const response = await fetch(`/api/task/${taskId}/solution/upload`, {
        method: "POST",
        body: data
    });

    if (!response.ok) {
        throw new Error(`Upload failed: ${response.status}`);
    }

    const result = await response;
    console.log(result);
}

async function getArtifactState(id : number) {
    const response = await fetch(`/api/artifact/${id}/state`);

    if (!response.ok) return null;

    const state : string = await response.json();
    return state;
}

export default function TaskPage() {
    const { id } = useParams<{ id : string }>();
    const intId = id ? parseInt(id) : 0;
    const [task, setTask] = useState<Task | null>(null);
    const [uploaded, setUploaded] = useState<boolean>(false);
    const [solution, setSolution] = useState<Artifact | null>(null);
    const [artifactState, setArtifactState] = useState<string | null>(null);

    useEffect(() => {
        if (!id) return;
        
        getTask(intId).then(t => setTask(t));
        getSolution(intId).then(s => setSolution(s));
    }, []);

    useEffect(() => {
        if (!uploaded || !id) return;

        getSolution(intId).then(s => setSolution(s));
    }, [uploaded]);

    useEffect(() => {
        if (solution !== null)
            getArtifactState(solution.id).then(s => setArtifactState(s));
    }, [solution]);

    if (task === null) return <>Loading</>;

    return (
        <div id="assignmentPage">
            <section id="taskPage">
                <h1>{task.title}</h1>
                <div id="dates">
                    <span>{task.creationDate.toLocaleString()}</span>
                    &middot;
                    <span>{task.dueDate.toLocaleString()}</span>
                </div>
                <div id="content">
                    <div id="description">{task.description}</div>
                    <div className="sectionName">
                        <h3>Solution</h3>
                        {solution !== null && 
                            <>
                            &middot;
                            <span>uploaded on {solution.date.toLocaleString()}</span>
                            </>
                        }
                    </div>
                    {
                        solution == null ? 
                        (
                            <form onSubmit={d => {
                                d.preventDefault();
                                submitSolution(d.target, intId).then(() => setUploaded(true));
                            }}>
                                <input type="file" name="files" multiple required/>
                                <input type="submit"/>
                            </form>
                        )
                        :
                        (
                            <>
                            <ArtifactView uuid={solution.uuid} />
                            </>
                        )
                    }
                </div>
            </section>
            <section>
                <div className="sectionName">
                    <h2>Analysis</h2>
                    {artifactState !== null && 
                        <>
                        &middot;
                        <span>{artifactState}</span>
                        </>
                    }
                </div>
            </section>
        </div>
    )
}