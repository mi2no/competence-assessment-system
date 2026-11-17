import { useEffect, useState, use, Suspense } from "react";
import { codeToHtml, bundledLanguages } from "shiki";

import FileIcon, { flavor } from "./icons";

import './ArtifactView.css';
import useColorScheme from "./useColorScheme";

export type FileInfo = {
    name : string,
    size : number
}

async function getArtifactStructure(uuid : string) {
    const response = await fetch('/api/artifact/structure?' + new URLSearchParams({
        uuid : uuid
    }).toString());

    if (!response.ok) throw new Error(`Structure GET faildef: ${response.status}`);

    const files = await response.json();
    return files.map((f: FileInfo) => f);
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

function getLangFromFileName(name : string) {
    const extension = name.split(".").pop()?.toLowerCase() ?? "";
    return extension in bundledLanguages ? extension : "text";
}

const fileContentCache : Record<string, Promise<string>> = {};

function loadFileContent(uuid : string, path : string) {
    let content = fileContentCache[path];

    if (!content) {
        content = getArtifactFileContent(uuid, path);
        content.catch(() => delete fileContentCache[path]);
        fileContentCache[path] = content;
    }

    return content;
}

function CodeView({ name, code } : { name : string, code : string }) {
    const [html, setHtml] = useState("");
    const scheme = useColorScheme();

    useEffect(() => {
        codeToHtml(code, { 
            lang : getLangFromFileName(name), 
            theme : `catppuccin-${flavor[scheme]}`
        }).then(setHtml);
    }, [name, code, scheme]);

    return (
        <div className="codeView" dangerouslySetInnerHTML={{__html : html}}/>
    )
}

function FileContent({ uuid, path } : { uuid : string, path : string }) {
    const content = use(loadFileContent(uuid, path));
    return <CodeView name={path} code={content}/>
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
            {active !== null && (
                <Suspense fallback={<div className="codeView loading">Loading...</div>}>
                    <FileContent uuid={uuid} path={files[active].name}/>
                </Suspense>
            )}
        </div>
    )
}