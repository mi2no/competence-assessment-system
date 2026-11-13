import { useEffect, useState } from "react";

import FileIcon from "./icons";

import './ArtifactView.css';

export type FileInfo = {
    name : string,
    size : number,
    content : string
}

async function getArtifactStructure(uuid : string) {
    const response = await fetch('/api/artifact/structure?' + new URLSearchParams({
        uuid : uuid
    }).toString());

    if (!response.ok) throw new Error(`Structure GET faildef: ${response.status}`);

    const files = await response.json();
    return files.map(f => ({
        ...f,
        content : getArtifactFileContent(uuid, f.name)
    }) as FileInfo);
}

async function getArtifactFileContent(uuid : string, path : string) {
    const response = await fetch('/api/artifact/file?' + new URLSearchParams({
        uuid : uuid,
        path : path
    }).toString());

    if (!response.ok) throw new Error(`Structure GET faildef: ${response.status}`);

    const file : string = await response.text();
    return file;
}

export default function ArtifactView({ uuid } : { uuid : string }) {
    const [files, setFiles] = useState<FileInfo[]>([]);
    const [active, setActive] = useState<number | null>(null);

    useEffect(() => {
        getArtifactStructure(uuid).then(f => {
            setFiles(f);
            if (f.length > 0) setActive(0);
        });
    }, [uuid]);

    return (
        <div className="artifactView">
            <ul className="files">
                {files.map((f, id) => (
                    <div
                        key={f.name}
                        className={active === id ? "active" : undefined}
                        onClick={() => setActive(id)}
                    >
                        <FileIcon size={20} name={f.name}/>
                        <span>{f.name}</span>
                    </div>
                ))}
            </ul>
            <pre><code>
                {active === null ? <></> : <>{files[active].content}</>}
            </code></pre>
        </div>
    )
}