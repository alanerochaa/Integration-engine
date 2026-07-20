package br.com.buni.integration.core.mapper;

import br.com.buni.integration.core.connector.CepConnector;
import br.com.buni.integration.core.model.importrow.ClienteImportRow;
import br.com.buni.integration.core.model.request.ClienteRequest;
import br.com.buni.integration.core.model.response.CepResponse;
import br.com.buni.integration.core.util.DateUtils;
import br.com.buni.integration.core.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;


//Ajustar o mapper -
@Component
@RequiredArgsConstructor
public class ClienteMapper {

    private final CepConnector cepConnector;

    public ClienteRequest toRequest(ClienteImportRow csv) {

        ClienteRequest request = new ClienteRequest();

        String cpf = resolverCpf(csv);
        Double renda = resolverRenda(csv.getSalario());

        request.setTipoPessoa("1");
        request.setCpf(cpf);
        request.setNome(resolverNome(csv.getNome()));
        request.setNacionalidade("1");
        request.setTipoDocumento("RG");
        request.setNumeroDocumento(cpf);
        // Campos fixos (não existem no CSV) apenas em HML Por hora
        request.setUfDocumento("SP");
        request.setDataEmissaoDocumento("2015-06-18T19:54:03Z");
        request.setOrgaoEmissorDocumento("SSP");
        request.setEstadoCivil("1");
        request.setNaturalidade("1");
        request.setSexo(resolverSexo(csv.getSexo()));
        request.setDataNascimento(DateUtils.formatarDataBrParaIso(csv.getDataNascimento()));
        request.setValorRenda(renda);
        request.setPep(false);
        request.setEscolaridade(resolverEscolaridade(csv.getEscolaridade()));
        request.setEmail(resolverEmail(csv));
        request.setDadoEndereco(montarEndereco(csv));
        request.setTelefones(montarTelefones(csv));
        request.setDadoProfissional(montarProfissional(csv));
        request.setDadosBancarios(montarDadosBancarios(csv));
        return request;
    }

    private String resolverCpf(ClienteImportRow csv) {

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
        if (StringUtils.vazio(nome)) return "";
        return nome.trim();
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
    private ClienteRequest.DadoEndereco montarEndereco(ClienteImportRow csv) {

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

    private ClienteRequest.Telefones montarTelefones(ClienteImportRow csv) {

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

    private ClienteRequest.DadoProfissional montarProfissional(ClienteImportRow csv) {

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

    private ClienteRequest.DadosBancarios montarDadosBancarios(ClienteImportRow csv) {

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

        if (StringUtils.vazio(agencia)) {
            return "0001";
        }

        String limpa = agencia
                .trim()
                .replaceAll("[^0-9]", "");

        if (limpa.isBlank()) {
            return "0001";
        }

        if (limpa.length() > 4) {
            limpa = limpa.substring(limpa.length() - 4);
        }

        return String.format("%4s", limpa).replace(' ', '0');
    }
    /**
     * CODBANCOPAGTO tem prioridade (código COMPE numérico).
     * Se vazio/não-numérico, tenta BANCO somente se for numérico.
     * Texto como "B.Uni" é descartado — o validador já terá gerado erro claro.
     */
    private String resolverCodigoBanco(ClienteImportRow csv) {

        String codBancoPagto = StringUtils.limparNumero(csv.getCompensacao());

        if (!StringUtils.vazio(codBancoPagto)) {
            return padCodigoBanco(codBancoPagto);
        }

        String banco = StringUtils.limparNumero(csv.getBanco());

        if (!StringUtils.vazio(banco)) {
            return padCodigoBanco(banco);
        }

        return "530";
    }

    private String padCodigoBanco(String codigo) {
        try {
            return String.format("%03d", Integer.parseInt(codigo));
        } catch (NumberFormatException e) {
            return codigo;
        }
    }


    private void registrarFallbackBancario(ClienteImportRow csv) {

        String observacaoAtual =
                csv.getObservacaoEndereco();

        if (StringUtils.vazio(observacaoAtual)) {
            observacaoAtual = "";
        }

        String observacaoBancaria =
                "AgenciaDigito vazio aplicado por fallback";

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

        String conta = contaPagamento.trim();

        // remove tudo que não for número
        conta = conta.replaceAll("[^0-9]", "");

        if (conta.isBlank()) {
            return "0000000001";
        }

        if (conta.length() > 10) {
            conta = conta.substring(conta.length() - 10);
        }

        return String.format("%10s", conta).replace(' ', '0');
    }

    private String resolverContaDigito(String contaPagamento) {

        if (StringUtils.vazio(contaPagamento)) {
            return "0";
        }

        String valor = contaPagamento.trim();

        if (valor.contains("-")) {
            return valor.substring(valor.lastIndexOf("-") + 1).trim();
        }

        if (valor.contains("/")) {
            return valor.substring(valor.lastIndexOf("/") + 1).trim();
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

    private String resolverEmail(ClienteImportRow csv) {

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

    private String resolverTelefone(ClienteImportRow csv) {

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

        final double RENDA_FALLBACK = 1518.00;

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

            double renda = Double.parseDouble(valorNormalizado);

            return renda > 0 ? renda : RENDA_FALLBACK;

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