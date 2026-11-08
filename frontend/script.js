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