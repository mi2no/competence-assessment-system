import './tasks.css';
import { Link } from "react-router-dom";
import { useState, useEffect } from 'react';

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
    const [tasks, setTasks] = useState<Task[]>([]);
    
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
            <Link to={"/create"}>
                <span>New assignment</span>
            </Link>
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
            <ul id="queue"></ul>
        </>
    )
}