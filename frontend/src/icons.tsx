import useColorScheme from "./useColorScheme";
import mapping from "./assets/icons/theme.json";

interface FileIconProps {
    name: string;
    size?: number;
}

const fileExtensions = mapping.fileExtensions as Record<string, string>;
const fileNames = mapping.fileNames as Record<string, string>;

// import.meta.glob jest rozwijane statycznie przez Vite w czasie budowania,
// więc zarówno wzorzec, jak i obiekt opcji muszą być literałami (bez zmiennych).
const iconSets = {
    light: import.meta.glob("/src/assets/icons/latte/*.svg", {
        eager: true,
        query: "?url",
        import: "default"
    }) as Record<string, string>,
    dark: import.meta.glob("/src/assets/icons/frappe/*.svg", {
        eager: true,
        query: "?url",
        import: "default"
    }) as Record<string, string>
};

const flavor = {
    light: "latte",
    dark: "frappe"
};

export default function FileIcon({ name, size = 24 }: FileIconProps) {
    const scheme = useColorScheme();
    const lower = name.toLowerCase();

    const iconName = fileNames[lower] || fileExtensions[lower.split(".").pop() ?? ""] || mapping.file;

    const path = `/src/assets/icons/${flavor[scheme]}/${iconName}.svg`;
    const src = iconSets[scheme][path];

    if (!src)
        return <img
            src={`/src/assets/icons/${flavor[scheme]}/_file.svg`}
            width={size}
            height={size}
        />;

    return <img src={src} width={size} height={size} />;
}
