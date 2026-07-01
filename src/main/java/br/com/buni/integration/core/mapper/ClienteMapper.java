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

        String cpf = resolverCpf(csv);

        Double renda = resolverRenda(csv.getSalario());

        // Garante que nunca seja enviada renda inválida
        if (renda == null || renda <= 0) {
            renda = 1518.00;
        }

        request.setTipoPessoa("1");
        request.setCpf(cpf);
        request.setNome(resolverNome(csv.getNome()));
        request.setNacionalidade("1");
        request.setTipoDocumento("RG");
        request.setNumeroDocumento(cpf);
        request.setUfDocumento("SP"); // fixo: CSV não possui UF do documento
        request.setDataEmissaoDocumento("2015-06-18T19:54:03Z"); // fixo: CSV não possui data de emissão
        request.setOrgaoEmissorDocumento("SSP");
        request.setEstadoCivil("1"); // padrão fixo (SOLTEIRO): CSV não possui estado civil
        request.setSexo(resolverSexo(csv.getSexo()));
        request.setDataNascimento(DateUtils.formatarDataBrParaIso(csv.getDataNascimento()));
        request.setValorRenda(renda);
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

    private String resolverCpf(ClienteCsv csv) {

        String cpf = StringUtils.limparNumero(csv.getCpf());

        // CPF informado no CSV
        if (!StringUtils.vazio(cpf)) {

            // Completa com zeros à esquerda quando vier menor que 11 dígitos
            if (cpf.length() < 11) {
                cpf = String.format("%11s", cpf).replace(' ', '0');
            }

            // Segurança caso venha maior que 11
            if (cpf.length() > 11) {
                cpf = cpf.substring(0, 11);
            }

            return cpf;
        }

        // CPF vazio -> gera um CPF válido baseado na matrícula do cliente
        String base = StringUtils.limparNumero(csv.getNumeroMatricula());

        // Caso a matrícula esteja vazia, utiliza o código da seção
        if (StringUtils.vazio(base)) {
            base = StringUtils.limparNumero(csv.getCodSecao());
        }

        // Caso ainda esteja vazio, utiliza um valor padrão
        if (StringUtils.vazio(base)) {
            base = "1";
        }

        // Mantém apenas os últimos 9 dígitos
        if (base.length() > 9) {
            base = base.substring(base.length() - 9);
        }

        // Completa com zeros à esquerda até formar 9 dígitos
        base = String.format("%9s", base).replace(' ', '0');

        return gerarCpfValido(base);
    }


    private String resolverNome(String nome) {

        if (StringUtils.vazio(nome)) {
            return "";
        }

        return nome
                .trim()
                .replace("'", "")
                .replace("\"", "");
    }
    private String gerarCpfValido(String base9) {

        int soma = 0;

        for (int i = 0; i < 9; i++) {
            soma += Character.getNumericValue(base9.charAt(i)) * (10 - i);
        }

        int resto = soma % 11;
        int digito1 = resto < 2 ? 0 : 11 - resto;

        String base10 = base9 + digito1;

        soma = 0;

        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(base10.charAt(i)) * (11 - i);
        }

        resto = soma % 11;
        int digito2 = resto < 2 ? 0 : 11 - resto;

        return base10 + digito2;
    }
    private ClienteRequest.DadoEndereco montarEndereco(ClienteCsv csv) {

        ClienteRequest.Endereco endereco =
                new ClienteRequest.Endereco();

        endereco.setTipo("1");
        endereco.setCep(StringUtils.limparNumero(csv.getCep()));
        endereco.setLogradouro(limitar(csv.getLogradouro(), 35));
        endereco.setNumero(csv.getNumero());

        CepResponse cepResponse =
                cepConnector.buscarCep(csv.getCep());

        if (cepResponse != null) {

            endereco.setBairro(cepResponse.getBairro());
            endereco.setCidade(cepResponse.getLocalidade());
            endereco.setUf(cepResponse.getUf());

            if (StringUtils.vazio(endereco.getLogradouro())) {
                endereco.setLogradouro(limitar(cepResponse.getLogradouro(), 35));
            }

            csv.setEnderecoFallbackAplicado(false);
            csv.setObservacaoEndereco("Endereco enriquecido via CEP");

        } else {

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

        enderecos.setEndereco(List.of(endereco));

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
       //sempre 4 sig.cel caso seja outro tipo de numero só ajustar via codigo
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

        profissional.setEmpresaNome(limitar(csv.getEmpresaNome(), 40));
        profissional.setEmpresaCnpj(StringUtils.limparNumero(csv.getEmpresaCnpj()));

        profissional.setProfissao(limitar(csv.getCargo(), 40));
        profissional.setCargo(limitar(csv.getCargo(), 20));

        profissional.setDataAdmissao(formatarDataSomenteData(csv.getDataAdmissao()));
        profissional.setNumeroMatricula(csv.getNumeroMatricula());

        return profissional;
    }

    private String limitar(String valor, int tamanho) {

        if (StringUtils.vazio(valor)) {
            return "";
        }

        valor = valor.trim();

        if (valor.length() <= tamanho) {
            return valor;
        }

        return valor.substring(0, tamanho);
    }

    private ClienteRequest.DadosBancarios montarDadosBancarios(ClienteCsv csv) {

        ClienteRequest.DadoBancario dado =
                new ClienteRequest.DadoBancario();

        String codigoBanco = resolverCodigoBanco(csv);
        dado.setCompensacao(codigoBanco);
        dado.setBanco(codigoBanco);
        dado.setAgencia(resolverAgencia(csv.getAgencia()));

        dado.setAgenciaDigito(resolverAgenciaDigito());

        dado.setConta(resolverConta(csv.getConta()));
        dado.setContaDigito(resolverContaDigito(csv.getConta()));

        // Campo obrigatório na API
        dado.setTipoConta("1"); // Conta Corrente

        registrarFallbackBancario(csv);

        ClienteRequest.DadosBancarios dados =
                new ClienteRequest.DadosBancarios();

        dados.setDadoBancario(List.of(dado));

        return dados;
    }

    private String resolverAgenciaDigito() {
        return "0";
    }

    private String resolverAgencia(String agencia) {

        String agenciaLimpa = StringUtils.limparNumero(agencia);

        if (StringUtils.vazio(agenciaLimpa)) {
            return "0001";
        }

        if (agenciaLimpa.length() < 4) {
            agenciaLimpa = String.format("%4s", agenciaLimpa).replace(' ', '0');
        }

        if (agenciaLimpa.length() > 4) {
            agenciaLimpa = agenciaLimpa.substring(agenciaLimpa.length() - 4);
        }

        return agenciaLimpa;
    }

    /**
     * CODBANCOPAGTO tem prioridade (código COMPE numérico).
     * Se vazio/não-numérico, tenta BANCO somente se for numérico.
     * Texto como "B.Uni" é descartado — o validador já terá gerado erro claro.
     */
    private String resolverCodigoBanco(ClienteCsv csv) {

        String codBancoPagto = StringUtils.limparNumero(csv.getCompensacao());

        if (!StringUtils.vazio(codBancoPagto)) {
            return codBancoPagto;
        }

        String banco = StringUtils.limparNumero(csv.getBanco());

        if (!StringUtils.vazio(banco)) {
            return banco;
        }

        return "530";
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
            return "0000000001";
        }

        String contaLimpa = contaPagamento.trim();

        if (contaLimpa.contains("-")) {
            contaLimpa = StringUtils.limparNumero(
                    contaLimpa.substring(0, contaLimpa.lastIndexOf("-"))
            );
        } else {
            contaLimpa = StringUtils.limparNumero(contaLimpa);
        }

        if (StringUtils.vazio(contaLimpa)) {
            return "0000000001";
        }

        if (contaLimpa.length() < 10) {
            contaLimpa = String.format("%10s", contaLimpa).replace(' ', '0');
        }

        if (contaLimpa.length() > 10) {
            contaLimpa = contaLimpa.substring(contaLimpa.length() - 10);
        }

        return contaLimpa;
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

        String cpf = resolverCpf(csv);

        String email = null;

        // Prioriza o e-mail pessoal
        if (!StringUtils.vazio(csv.getEmailPessoal())) {
            email = csv.getEmailPessoal().trim();
        }
        // Senão utiliza o profissional
        else if (!StringUtils.vazio(csv.getEmailProfissional())) {
            email = csv.getEmailProfissional().trim();
        }

        // Não possui e-mail
        if (StringUtils.vazio(email)) {
            return cpf + "@fallback.local";
        }

        // Não possui @
        if (!email.contains("@")) {
            return cpf + "@fallback.local";
        }

        String usuario = email.substring(0, email.indexOf("@"));

        // Login inicia com número (caso encontrado no CSV)
        if (!usuario.isEmpty() && Character.isDigit(usuario.charAt(0))) {
            return cpf + "@fallback.local";
        }

        return email.toLowerCase();
    }

    private String resolverTelefone(ClienteCsv csv) {

        String telefone1 = StringUtils.limparNumero(csv.getTelefone1());
        String telefone2 = StringUtils.limparNumero(csv.getTelefone2());

        // Prioriza Telefone1
        if (!StringUtils.vazio(telefone1) && telefone1.length() >= 10) {
            return telefone1;
        }

        // Caso Telefone1 esteja vazio ou inválido, utiliza Telefone2
        if (!StringUtils.vazio(telefone2) && telefone2.length() >= 10) {
            return telefone2;
        }

        // Fallback para clientes sem telefone no CSV
        return "11999999999";
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

        final Double RENDA_FALLBACK = 1518.00;

        if (StringUtils.vazio(salario)) {
            return RENDA_FALLBACK;
        }

        try {

            String valorNormalizado = salario
                    .trim()
                    .replace("R$", "")
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", ".");

            Double renda = Double.parseDouble(valorNormalizado);

            if (renda == null || renda <= 0) {
                return RENDA_FALLBACK;
            }

            return renda;

        } catch (Exception e) {
            return RENDA_FALLBACK;
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