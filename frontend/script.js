let currentCode = "";

const codeArea = document.getElementById("code");
const resultArea = document.getElementById("result");
const fileInput = document.getElementById("fileInput");

// Open File
document.getElementById("openBtn").addEventListener("click", () => {
    fileInput.click();
});

fileInput.addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (ev) => {
        currentCode = ev.target.result;
        codeArea.value = currentCode;

        resultArea.textContent = "File loaded successfully!\nClick \"Lexical Analysis\" to begin.";

        // Enable only Lexical
        document.getElementById("lexicalBtn").disabled = false;
        document.getElementById("syntaxBtn").disabled = true;
        document.getElementById("semanticBtn").disabled = true;

        // Reset progress
        document.querySelectorAll(".step").forEach(s => s.classList.remove("done"));
    };
    reader.readAsText(file);
});

// Analysis buttons
document.getElementById("lexicalBtn").onclick = () => runPhase("lexical");
document.getElementById("syntaxBtn").onclick = () => runPhase("syntax");
document.getElementById("semanticBtn").onclick = () => runPhase("semantic");

document.getElementById("clearBtn").onclick = () => {
    currentCode = "";
    codeArea.value = "";
    resultArea.textContent = "Click \"Open File\" to load your code and begin analysis.";
    document.getElementById("lexicalBtn").disabled = true;
    document.getElementById("syntaxBtn").disabled = true;
    document.getElementById("semanticBtn").disabled = true;
    document.querySelectorAll(".step").forEach(s => s.classList.remove("done"));
};

async function runPhase(phase) {
    const phaseName = phase === "lexical" ? "Lexical" : phase === "syntax" ? "Syntax" : "Semantic";

    resultArea.textContent = `Performing ${phaseName} Analysis...\n\nPlease wait...`;

    try {
        const response = await fetch("/analyze", {
            method: "POST",
            headers: {
                "Content-Type": "text/plain",
                "X-Phase": phase
            },
            body: currentCode
        });

        const text = await response.text();

        if (text.includes("PASSED")) {
            resultArea.textContent = `Performing ${phaseName} Analysis...\n${phaseName.toUpperCase()} ANALYSIS PASSED!`;

            // Mark step done
            document.getElementById(`step-${phase}`).classList.add("done");

            // Disable current button
            document.getElementById(`${phase}Btn`).disabled = true;

            // Enable next
            if (phase === "lexical") document.getElementById("syntaxBtn").disabled = false;
            if (phase === "syntax") document.getElementById("semanticBtn").disabled = false;
        } else {
            resultArea.textContent = text;
        }
    } catch (err) {
        console.error(err);
        resultArea.textContent = "ERROR: Could not connect to server.\n\nMake sure you ran:\njava com.minicompiler.MiniCompiler\n\nAnd the terminal is still open.";
    }
}