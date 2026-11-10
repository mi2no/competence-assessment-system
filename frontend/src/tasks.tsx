import './tasks.css';
import { useState, useEffect } from 'react';

type ArtifactEntry = {
    uuid : string,
    date : string,
    state : string
}

export default function Tasks() {
    const [artifacts, setArtifacts] = useState<ArtifactEntry[]>([])
    
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

    return (
        <>
            <form>
                <input type="file" name="files" multiple required/>
                <input type="submit"/>
            </form>
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
                            <>
                                <span>{entry.uuid}</span>
                                <span>{entry.date}</span>
                                <span>{entry.state}</span>
                            </>
                        ))}
                    </div>
                </section>
            </section>
            <ul id="queue"></ul>
        </>
    )
}