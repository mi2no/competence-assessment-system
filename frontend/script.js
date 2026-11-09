const form = document.querySelector("form");

form.addEventListener("submit", async (event) => {
    event.preventDefault();

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
});

const refresh = document.querySelector("button.refresh");
const queue = document.querySelector("#queue");

async function getArtifactStructure(uuid){
    const response = await fetch('/api/artifact/structure?' + new URLSearchParams({
        uuid: uuid
    }).toString(), {
        method: "GET"
    });

    if (!response.ok) throw new Error(`Structure GET faildef: ${response.status}`);

    return await response.json();
}

refresh.onclick = async event => {
    const response = await fetch("/api/artifact/queue", {
        method: "GET"
    });

    if (!response.ok) {
        throw new Error(`Upload failed: ${response.status}`);
    }

    await response.json().then(json => {
        console.log(json);
        json.map(entry => {
            const li = document.createElement("li");

            const uuid = document.createTextNode(entry.uuid);
            const date = document.createTextNode(entry.date);
            const state = document.createTextNode(entry.state);

            li.append(uuid, date, state);
            queue.append(li);

            console.log(entry.uuid);
            getArtifactStructure(entry.uuid).then(json => {
                console.log(json);
            });
        });
    });
};