package br.com.buni.integration.core.mapper;

import br.com.buni.integration.core.connector.CepConnector;
import br.com.buni.integration.core.model.csv.ClienteCsv;
import br.com.buni.integration.core.model.request.ClienteRequest;
import br.com.buni.integration.core.model.response.CepResponse;
import br.com.buni.integration.core.util.DateUtils;
import br.com.buni.integration.core.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClienteMapper {

    private final CepConnector cepConnector;

    public ClienteRequest toRequest(ClienteCsv csv) {

        ClienteRequest request = new ClienteRequest();

        request.setTipoPessoa("1");
        request.setCpf(StringUtils.normalizarCpf(csv.getCpf()));
        request.setNome(csv.getNome());
        request.setNacionalidade("1");
        request.setTipoDocumento("RG");
        request.setNumeroDocumento(StringUtils.normalizarCpf(csv.getCpf()));
        request.setUfDocumento("SP"); // fixo: CSV não possui UF do documento
        request.setDataEmissaoDocumento("2015-06-18T19:54:03Z"); // fixo: CSV não possui data de emissão
        request.setOrgaoEmissorDocumento("SSP");
        request.setEstadoCivil("1"); // padrão fixo (SOLTEIRO): CSV não possui estado civil
        request.setSexo(resolverSexo(csv.getSexo()));
        request.setDataNascimento(DateUtils.formatarDataBrParaIso(csv.getDataNascimento()));
        request.setValorRenda(resolverRenda(csv.getSalario()));
        request.setPep(false);
        request.setEscolaridade(resolverEscolaridade(csv.getEscolaridade()));
        request.setNaturalidade("1");
        request.setEmail(resolverEmail(csv));

        request.setDadoEndereco(montarEndereco(csv));
        request.setTelefones(montarTelefones(csv));
        request.setDadoProfissional(montarProfissional(csv));
        request.setDadosBancarios(montarDadosBancarios(csv));

        return request;
    }

    private ClienteRequest.DadoEndereco montarEndereco(ClienteCsv csv) {

        ClienteRequest.Endereco endereco =
                new ClienteRequest.Endereco();

        endereco.setTipo("1");
        endereco.setCep(StringUtils.limparNumero(csv.getCep()));
        endereco.setLogradouro(csv.getLogradouro());
        endereco.setNumero(csv.getNumero());

        CepResponse cepResponse =
                cepConnector.buscarCep(
                        csv.getCep()
                );

        if (cepResponse != null) {

            endereco.setBairro(cepResponse.getBairro());
            endereco.setCidade(cepResponse.getLocalidade());
            endereco.setUf(cepResponse.getUf());

            if (StringUtils.vazio(endereco.getLogradouro())) {
                endereco.setLogradouro(cepResponse.getLogradouro());
            }

            csv.setEnderecoFallbackAplicado(false);
            csv.setObservacaoEndereco("Endereco enriquecido via CEP");

        } else {

            // Fallback quando ViaCEP não retorna dados; UF padrão SP (escopo de homologação)
            endereco.setBairro("NAO INFORMADO");
            endereco.setCidade("NAO INFORMADO");
            endereco.setUf("SP");

            csv.setEnderecoFallbackAplicado(true);
            csv.setObservacaoEndereco(
                    "Fallback aplicado: Bairro/Cidade não localizados via CEP; UF=SP"
            );
        }

        ClienteRequest.Enderecos enderecos =
                new ClienteRequest.Enderecos();

        enderecos.setEndereco(
                List.of(endereco)
        );

        ClienteRequest.DadoEndereco dadoEndereco =
                new ClienteRequest.DadoEndereco();

        dadoEndereco.setTipoResidencia("1");
        dadoEndereco.setResidenciaAnteriorAnos("");
        dadoEndereco.setResidenciaAnteriorMeses("");
        dadoEndereco.setEnderecos(enderecos);

        return dadoEndereco;
    }

    private ClienteRequest.Telefones montarTelefones(ClienteCsv csv) {

        String telefone =
                resolverTelefone(csv);

        ClienteRequest.Telefone tel =
                new ClienteRequest.Telefone();

        tel.setTipo("4");
        tel.setDdd(extrairDdd(telefone));
        tel.setNumeroTelefone(extrairNumeroTelefone(telefone));

        ClienteRequest.Telefones telefones =
                new ClienteRequest.Telefones();

        telefones.setTelefone(
                List.of(tel)
        );

        return telefones;
    }

    private ClienteRequest.DadoProfissional montarProfissional(ClienteCsv csv) {

        ClienteRequest.DadoProfissional profissional =
                new ClienteRequest.DadoProfissional();

        profissional.setEmpresaNome(csv.getEmpresaNome());
        profissional.setEmpresaCnpj(StringUtils.limparNumero(csv.getEmpresaCnpj()));
        profissional.setProfissao(csv.getCargo());
        profissional.setCargo(csv.getCargo());
        profissional.setDataAdmissao(formatarDataSomenteData(csv.getDataAdmissao()));
        profissional.setNumeroMatricula(csv.getNumeroMatricula());

        return profissional;
    }

    private ClienteRequest.DadosBancarios montarDadosBancarios(ClienteCsv csv) {

        ClienteRequest.DadoBancario dado =
                new ClienteRequest.DadoBancario();

        dado.setCompensacao(StringUtils.limparNumero(csv.getCompensacao()));
        dado.setBanco(StringUtils.limparNumero(csv.getBanco()));
        dado.setAgencia(StringUtils.limparNumero(csv.getAgencia()));

        // Fallback HML: campo não veio no CSV
        dado.setAgenciaDigito(resolverAgenciaDigito());

        dado.setConta(resolverConta(csv.getConta()));
        dado.setContaDigito(resolverContaDigito(csv.getConta()));

        registrarFallbackBancario(csv);

        ClienteRequest.DadosBancarios dados =
                new ClienteRequest.DadosBancarios();

        dados.setDadoBancario(
                List.of(dado)
        );

        return dados;
    }

    private String resolverAgenciaDigito() {
        return "0";
    }

    private void registrarFallbackBancario(ClienteCsv csv) {

        String observacaoAtual =
                csv.getObservacaoEndereco();

        if (StringUtils.vazio(observacaoAtual)) {
            observacaoAtual = "";
        }

        String observacaoBancaria =
                "AgenciaDigito=0 aplicado por fallback em homologação";

        if (!observacaoAtual.contains(observacaoBancaria)) {

            if (!observacaoAtual.isBlank()) {
                observacaoAtual =
                        observacaoAtual + " | " + observacaoBancaria;
            } else {
                observacaoAtual =
                        observacaoBancaria;
            }

            csv.setObservacaoEndereco(observacaoAtual);
        }
    }

    private String resolverConta(String contaPagamento) {

        if (StringUtils.vazio(contaPagamento)) {
            return "";
        }

        String contaLimpa =
                contaPagamento.trim();

        if (contaLimpa.contains("-")) {
            return StringUtils.limparNumero(
                    contaLimpa.substring(
                            0,
                            contaLimpa.lastIndexOf("-")
                    )
            );
        }

        return StringUtils.limparNumero(contaLimpa);
    }

    private String resolverContaDigito(String contaPagamento) {

        if (StringUtils.vazio(contaPagamento)) {
            return "0";
        }

        String contaLimpa =
                contaPagamento.trim();

        if (contaLimpa.contains("-")) {
            return StringUtils.limparNumero(
                    contaLimpa.substring(
                            contaLimpa.lastIndexOf("-") + 1
                    )
            );
        }

        return "0";
    }

    private String resolverSexo(String sexo) {

        if (StringUtils.vazio(sexo)) {
            return "0";
        }

        return switch (sexo.trim().toUpperCase()) {
            case "M" -> "1";
            case "F" -> "2";
            default -> "0";
        };
    }

    private String resolverEscolaridade(String escolaridade) {

        if (StringUtils.vazio(escolaridade)) {
            return "0";
        }

        String valor = StringUtils.removerAcentos(escolaridade).toLowerCase();

        // Ordem importa: verifica níveis mais específicos primeiro
        if (valor.contains("mestrado") || valor.contains("doutorado")) return "6"; // MESTRADO_DOUTORADO
        if (valor.contains("pos") || valor.contains("especializacao"))  return "5"; // POS_GRADUACAO
        if (valor.contains("superior") || valor.contains("graduacao"))  return "3"; // SUPERIOR
        if (valor.contains("medio") || valor.contains("segundo"))       return "2"; // SEGUNDO_GRAU
        if (valor.contains("fundamental") || valor.contains("primario")
                || valor.contains("primeiro"))                          return "1"; // PRIMEIRO_GRAU

        return "0"; // NAO_DEFINIDO
    }

    private String resolverEmail(ClienteCsv csv) {

        if (!StringUtils.vazio(csv.getEmailPessoal())) {
            return csv.getEmailPessoal();
        }

        return csv.getEmailProfissional();
    }

    private String resolverTelefone(ClienteCsv csv) {

        if (!StringUtils.vazio(csv.getTelefone1())) {
            return StringUtils.limparNumero(csv.getTelefone1());
        }

        return StringUtils.limparNumero(csv.getTelefone2());
    }

    private String extrairDdd(String telefone) {

        if (telefone == null || telefone.length() < 10) {
            return "";
        }

        return telefone.substring(0, 2);
    }

    private String extrairNumeroTelefone(String telefone) {

        if (telefone == null || telefone.length() <= 2) {
            return "";
        }

        return telefone.substring(2);
    }

    private Double resolverRenda(String salario) {

        if (StringUtils.vazio(salario)) {
            return 0.0;
        }

        try {
            return Double.parseDouble(
                    salario
                            .replace(".", "")
                            .replace(",", ".")
                            .trim()
            );
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatarDataSomenteData(String data) {

        if (StringUtils.vazio(data)) {
            return "";
        }

        String apenasData =
                data
                        .trim()
                        .split(" ")[0];

        String[] partes =
                apenasData.split("/");

        if (partes.length != 3) {
            return data;
        }

        return partes[2] + "-" + partes[1] + "-" + partes[0];
    }
}