import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import './TaskPage.css'

type Task = {
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

type FileInfo = {
    name : string,
    size : number,
    content : string
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

async function submitSolution(form : HTMLFormElement) {
    const data = new FormData(form);
    const id = 1;

    const response = await fetch(`/api/task/${id}/solution/upload`, {
        method: "POST",
        body: data
    });

    if (!response.ok) {
        throw new Error(`Upload failed: ${response.status}`);
    }

    const result = await response;
    console.log(result);
}

async function getArtifactStructure(uuid : string) {
    const response = await fetch('/api/artifact/structure?' + new URLSearchParams({
        uuid : uuid
    }).toString());

    if (!response.ok) throw new Error(`Structure GET faildef: ${response.status}`);

    const files = await response.json();
    return files.map(f => ({
        ...f,
        content : getArtifactFile(uuid, f.name)
    }) as FileInfo);
}

async function getArtifactFile(uuid : string, path : string) {
    const response = await fetch('/api/artifact/file?' + new URLSearchParams({
        uuid : uuid,
        path : path
    }).toString());

    if (!response.ok) throw new Error(`Structure GET faildef: ${response.status}`);

    const file : string = await response.text();
    return file;
}

export default function TaskPage() {
    const { id } = useParams<{ id: string }>();
    const [task, setTask] = useState<Task | null>(null);
    const [solution, setSolution] = useState<Artifact | null>(null);
    const [files, setFiles] = useState<FileInfo[] | null>(null);

    useEffect(() => {
        if (!id) return;
        
        const intId = parseInt(id);
        
        getTask(intId).then(t => setTask(t));
        getSolution(intId).then(s => setSolution(s));
    }, []);

    useEffect(() => {
        if (solution != null) {
            console.log(solution.uuid);
            getArtifactStructure(solution.uuid).then(
                s => setFiles(s)
            );
        }
    }, [solution])

    if (task == null) return <>Loading</>;

    return (
        <section id="taskPage">
            <h1>{task.title}</h1>
            <div id="dates">
                <span>{task.creationDate.toLocaleString()}</span>
                &middot;
                <span>{task.dueDate.toLocaleString()}</span>
            </div>
            <div id="content">
                <div id="description">{task.description}</div>
                <h3>Solution</h3>
                {
                    solution == null ? 
                    (
                        <form onSubmit={d => {
                            d.preventDefault();
                            submitSolution(d.target);
                        }}>
                            <input type="file" name="files" multiple required/>
                            <input type="submit"/>
                        </form>
                    )
                    :
                    (
                        <>
                        <span>Uploaded on {solution.date.toLocaleString()}</span>
                        <span>{solution.uuid}</span>
                        {files?.map(f => (
                            <>
                                <span>{f.name}</span>
                                <pre><code>{f.content}</code></pre>
                            </>
                        ))}
                        </>
                    )
                }
            </div>
        </section>
    )
}