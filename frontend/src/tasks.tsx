import './tasks.css';
import { Link } from "react-router-dom";
import { useState, useEffect, Fragment } from 'react';

type ArtifactEntry = {
    uuid : string,
    date : string,
    state : string
}

type Task = {
    id : number,
    title : string,
    description : string,
    creationDate : Date,
    dueDate : Date,
    teacherId : number,
    courseId : number
}

async function getTasks() {
    const response = await fetch('/api/task/get');

    if (!response.ok) throw new Error(`Fetch failed: ${response.status}`);

    const tasks = await response.json();
    return tasks.map(t => ({
        ...t,
        creationDate : new Date(t.creationDate),
        dueDate: new Date(t.dueDate)
    }) as Task);
}

export default function Tasks() {
    const [artifacts, setArtifacts] = useState<ArtifactEntry[]>([])
    const [tasks, setTasks] = useState<Task[]>([]);
    
    async function getArtifactEntries() {
        const response = await fetch('/api/artifact/queue');

        if (!response.ok) {
            throw new Error(`Fetch failed: ${response.status}`);
        }

        const json: ArtifactEntry[] = await response.json();
        setArtifacts(json);
    }
    
    useEffect(() => {
        getArtifactEntries();
    }, []);

    useEffect(() => {
        getTasks().then(arr => setTasks(arr));
    }, []);

    return (
        <>
            <ul id="statusViews">
                <button>Upcoming</button>
                <button>Overdue</button>
                <button>Done</button>
            </ul>
            <ul id="taskList">
                {tasks.map(task => (
                    <Link to={`/task/${task.id}`} key={task.id} className="service">
                        <div className="task">
                            <h2>{task.title}</h2>
                            <span>{task.dueDate.toDateString()}</span>
                        </div>
                    </Link>
                ))}
            </ul>
            <section>
                <div>
                    <h2>Uploaded Solutions</h2>
                    <button className="refresh" onClick={getArtifactEntries}></button>
                </div>
                <section className="solutions">
                    <h3>Personal</h3>
                    <div>
                        <span>UUID</span>
                        <span>Date</span>
                        <span>State</span>
                        {artifacts.map(entry => (
                            <Fragment key={entry.uuid}>
                                <span>{entry.uuid}</span>
                                <span>{entry.date}</span>
                                <span>{entry.state}</span>
                            </Fragment>
                        ))}
                    </div>
                </section>
            </section>
            <ul id="queue"></ul>
        </>
    )
}