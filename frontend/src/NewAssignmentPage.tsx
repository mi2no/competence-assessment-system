import './NewAssignmentPage.css'

type TaskForm = {
    title : string,
    description : string,
    active : Date,
    due : Date,
    teacherId : number,
    courseId : number
}

async function createAssignment(form : HTMLFormElement) {
    const data = new FormData(form);
    const task = {
        title : data.get("title") as string,
        description : data.get("description") as string,
        active : data.get("active") as Date | null,
        due : data.get("due") as Date | null,
        teacherId : 1,
        courseId : 1
    } as TaskForm;

    console.log(task);

    const response = await fetch("/api/task/create", {
        method: "POST",
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(task)
    });

    if (!response.ok) {
        throw new Error(`Creation failed: ${response.status}`);
    }
}

export default function NewAssignmentPage() {
    return (
        <form id="newAssignment" onSubmit={e => {
            e.preventDefault();
            createAssignment(e.target)
        }}>
            <input type="text" name="title" required></input>
            <textarea name="description" required></textarea>
            <input type="datetime-local" name="active" required></input>
            <input type="datetime-local" name="due" required></input>
            <input type="submit"></input>
        </form>
    )
}