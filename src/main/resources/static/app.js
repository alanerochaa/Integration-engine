async function importarArquivo() {
    const fileInput        = document.getElementById("fileInput");
    const statusBannerEl   = document.getElementById("statusBanner");
    const statusBannerTxt  = document.getElementById("statusBannerText");
    const status           = document.getElementById("status");
    const arquivo          = document.getElementById("arquivo");
    const totalLinhas      = document.getElementById("totalLinhas");
    const totalSucesso     = document.getElementById("totalSucesso");
    const totalErro        = document.getElementById("totalErro");
    const totalDuplicado   = document.getElementById("totalDuplicado");
    const relatorio        = document.getElementById("relatorio");
    const downloadBtn      = document.getElementById("downloadBtn");
    const excelBtn         = document.getElementById("excelBtn");
    const btnImportar      = document.getElementById("btnImportar");
    const tempoResposta    = document.getElementById("tempoResposta");

    if (!fileInput.files || fileInput.files.length === 0) {
        statusBannerEl.className       = "status-banner warning";
        statusBannerTxt.textContent    = "Selecione um arquivo para processamento.";
        status.textContent             = "Aguardando envio";
        return;
    }

    const inicio   = performance.now();
    const file     = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);

    btnImportar.disabled = true;
    btnImportar.classList.add("loading");

    statusBannerEl.className       = "status-banner processing";
    statusBannerTxt.textContent = "Processando arquivo...";
    status.textContent             = "Em processamento";
    arquivo.textContent            = file.name;

    totalLinhas.textContent    = "—";
    totalSucesso.textContent   = "—";
    totalErro.textContent      = "—";
    totalDuplicado.textContent = "—";
    relatorio.textContent      = "—";

    totalErro.className      = "value metric error";
    totalDuplicado.className = "value metric warning";

    if (tempoResposta) tempoResposta.textContent = "—";

    if (downloadBtn) { downloadBtn.style.display = "none"; downloadBtn.removeAttribute("href"); }
    if (excelBtn)    { excelBtn.style.display    = "none"; excelBtn.removeAttribute("href"); }

    try {
        const response = await fetch("/importar/clientes", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            const erroTexto = await response.text();
            throw new Error(erroTexto || `Erro HTTP ${response.status}`);
        }

        const data = await response.json();
        const fim  = performance.now();

        if (tempoResposta) {
            tempoResposta.textContent = `${((fim - inicio) / 1000).toFixed(2)}s`;
        }

        const statusLabels = {
            'SUCESSO':                    'Sucesso',
            'PROCESSADO_COM_DUPLICIDADES':'Processado com duplicidades',
            'PROCESSADO_COM_ALERTAS':     'Processado com alertas',
            'FALHA':                      'Falha'
        };
        status.textContent = statusLabels[data.status] || data.status || "Processamento finalizado";

        const erros      = data.totalErro      ?? 0;
        const duplicados = data.totalDuplicado ?? 0;

        totalLinhas.textContent    = data.totalLinhas  ?? 0;
        totalSucesso.textContent   = data.totalSucesso ?? 0;
        totalErro.textContent      = erros;
        totalDuplicado.textContent = duplicados;

        totalErro.className      = `value metric${erros      > 0 ? ' error'   : ''}`;
        totalDuplicado.className = `value metric${duplicados > 0 ? ' warning' : ''}`;

        if (erros > 0 || duplicados > 0) {
            statusBannerEl.className    = "status-banner warning";
            statusBannerTxt.textContent = "Processamento concluído com ocorrências. Verifique o relatório.";
        } else {
            statusBannerEl.className    = "status-banner success";
            statusBannerTxt.textContent = "Arquivo processado com sucesso.";
        }

        relatorio.textContent = "Relatório gerado";

        if (data.downloadUrl && downloadBtn) {
            downloadBtn.href          = data.downloadUrl;
            downloadBtn.target        = "_blank";
            downloadBtn.style.display = "inline-flex";
        }
        if (data.excelUrl && excelBtn) {
            excelBtn.href          = data.excelUrl;
            excelBtn.style.display = "inline-flex";
        }

    } catch (error) {
        console.error("Erro no processamento:", error);

        statusBannerEl.className    = "status-banner error";
        statusBannerTxt.textContent = "Falha técnica ao integrar com o backend.";
        status.textContent          = "Erro no processamento";
        totalErro.textContent       = "1";
        relatorio.textContent       = error.message || "Erro não identificado.";

    } finally {
        btnImportar.disabled = false;
        btnImportar.classList.remove("loading");
    }
}