import useColorScheme from "./useColorScheme";

interface FileIconProps {
    extension: string;
    size?: number;
}

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

export default function FileIcon({ extension, size = 24 }: FileIconProps) {
    const scheme = useColorScheme();

    const path = `/src/assets/icons/${flavor[scheme]}/${extension.toLowerCase()}.svg`;
    const src = iconSets[scheme][path];

    if (!src)
        return <img
            src={`/src/assets/icons/${flavor[scheme]}/_file.svg`}
            width={size}
            height={size}
        />;

    return <img src={src} width={size} height={size} />;
}
