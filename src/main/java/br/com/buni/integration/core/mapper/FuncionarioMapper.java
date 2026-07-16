package br.com.buni.integration.core.mapper;

import br.com.buni.integration.core.model.importrow.FuncionarioImportRow;
import br.com.buni.integration.core.model.request.FuncionarioRequest;
import br.com.buni.integration.core.util.DateUtils;
import br.com.buni.integration.core.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class FuncionarioMapper {

    private static final Set<String> UFS_VALIDAS = Set.of(
        "AC","AL","AM","AP","BA","CE","DF","ES","GO","MA","MG","MS","MT",
        "PA","PB","PE","PI","PR","RJ","RN","RO","RR","RS","SC","SE","SP","TO"
    );

    public FuncionarioRequest toRequest(FuncionarioImportRow row) {

        String cpf        = resolverCpf(row.getCpf());
        double rendaBruta = resolverDouble(row.getRendaBruta());

        FuncionarioRequest request = new FuncionarioRequest();
        request.setDadosPessoais(montarDadosPessoais(row, cpf));
        request.setContato(montarContato(row, cpf));
        request.setDadosEmpresa(montarDadosEmpresa(row));
        request.setDadosBancarios(montarDadosBancarios(row));
        request.setRendasMargem(List.of(montarRendaMargem(row, rendaBruta)));
        request.setOrigem4(montarOrigem4(row));
        request.setOrigem5(montarOrigem5(row));

        return request;
    }

    // ─── DadosPessoais ───────────────────────────────────────────────────────────

    private FuncionarioRequest.DadosPessoais montarDadosPessoais(FuncionarioImportRow row, String cpf) {

        FuncionarioRequest.DadosPessoais dp = new FuncionarioRequest.DadosPessoais();

        dp.setCpf(cpf);
        dp.setNomeFuncionario(truncar(limpar(row.getNomeFuncionario()), 60));
        dp.setNomePai(truncar(limpar(row.getNomePai()), 60));
        dp.setNomeMae(truncar(limpar(row.getNomeMae()), 60));
        dp.setDataNascimento(resolverDataOpcional(row.getDataNascimento()));
        dp.setDataCadastro(resolverDataCadastro(row, cpf));
        dp.setTipoDocumento(resolverTipoDocumento(row.getTipoDocumento()));
        dp.setNumeroDocumento(truncar(resolverNumeroDocumento(row.getRg(), cpf), 14));
        dp.setOrgaoEmissor(truncar(resolverOrgaoEmissor(row.getOrgaoEmissor()), 10));
        dp.setUf(resolverUf(row.getUfDocumento()));
        dp.setDataEmissaoDocumento(DateUtils.formatarDataFuncionario(row.getDataEmissaoDocumento()));
        dp.setSexo(resolverSexo(row.getSexo()));
        dp.setEstadoCivil(resolverEstadoCivil(row.getEstadoCivil()));
        dp.setNaturalidade(truncar(limpar(row.getNaturalidade()), 40));
        dp.setNacionalidade(resolverNacionalidade(row.getNacionalidade()));
        dp.setCodNacionalidade(resolverCodNacionalidade(row.getCodNacionalidade()));
        dp.setId("");

        return dp;
    }

    // ─── Contato ─────────────────────────────────────────────────────────────────

    private FuncionarioRequest.Contato montarContato(FuncionarioImportRow row, String cpf) {

        FuncionarioRequest.Contato contato = new FuncionarioRequest.Contato();
        contato.setEmail(resolverEmail(row.getEmail(), cpf));
        contato.setDddTel("");
        contato.setTel("");
        contato.setDddCom("");
        contato.setTelCom("");
        contato.setDddCel(StringUtils.limparNumero(row.getDddCel()));
        contato.setCelular(StringUtils.limparNumero(row.getCelular()));

        FuncionarioRequest.Endereco endereco = new FuncionarioRequest.Endereco();
        endereco.setEndereco(truncar(limpar(row.getLogradouro()), 60));
        endereco.setCep(resolverCep(row.getCep()));
        endereco.setCidade(truncar(limpar(row.getCidade()), 40));
        endereco.setUf(resolverUf(row.getUf()));
        endereco.setNumero(resolverNumeroEndereco(row.getNumero()));
        endereco.setComplemento(truncar(limpar(row.getComplemento()), 20));
        endereco.setBairro(truncar(limpar(row.getBairro()), 40));
        contato.setEndereco(endereco);

        return contato;
    }

    // ─── DadosEmpresa ────────────────────────────────────────────────────────────

    private FuncionarioRequest.DadosEmpresa montarDadosEmpresa(FuncionarioImportRow row) {

        FuncionarioRequest.DadosEmpresa empresa = new FuncionarioRequest.DadosEmpresa();
        empresa.setCargo(truncar(limpar(row.getCargo()), 40));
        empresa.setTpVinculo(resolverTpVinculo(row.getTpVinculo()));
        empresa.setMatricula(truncar(limpar(row.getMatricula()), 20));
        empresa.setSituacao(resolverSituacao(row.getSituacao()));
        empresa.setDataAdmissao(DateUtils.formatarDataFuncionario(row.getDataAdmissao()));

        return empresa;
    }

    // ─── DadosBancarios ──────────────────────────────────────────────────────────

    private FuncionarioRequest.DadosBancarios montarDadosBancarios(FuncionarioImportRow row) {

        FuncionarioRequest.DadosBancarios banco = new FuncionarioRequest.DadosBancarios();
        banco.setCodBanco(resolverCodBanco(row.getBanco()));
        banco.setTipodeConta(resolverTipoConta(row.getTipoDeConta()));
        banco.setNrAgencia(resolverAgencia(row.getAgencia()));
        banco.setAgenciaDig("0");
        banco.setNrConta(resolverConta(row.getConta()));
        banco.setContaDig(resolverDigitoConta(row.getDigitoConta()));

        return banco;
    }

    // ─── RendaMargem ─────────────────────────────────────────────────────────────

    private FuncionarioRequest.RendaMargem montarRendaMargem(FuncionarioImportRow row, double rendaBruta) {

        double rendaLiquida = resolverDouble(row.getRendaLiquida());
        if (rendaLiquida <= 0.0) {
            rendaLiquida = rendaBruta;
        }

        FuncionarioRequest.RendaMargem renda = new FuncionarioRequest.RendaMargem();
        renda.setTipoVinculo("C");
        renda.setRendaBruta(rendaBruta);
        renda.setRendaLiquida(rendaLiquida);
        renda.setMargemTotalParcelada(0.0);
        renda.setMargemDispParcelar(0.0);
        renda.setMargemTotalCartao(0.0);
        renda.setMargemDispCartao(0.0);
        renda.setRendaBrutaCript("");
        renda.setRendaLiquidaCript("");
        renda.setMargemTotalParceladaCript("");
        renda.setMargemDispParcelarCript("");
        renda.setMargemTotalCartaoCript("");
        renda.setMargemDispCartaoCript("");
        renda.setIdFunc(0);

        return renda;
    }

    // ─── Origens ─────────────────────────────────────────────────────────────────

    private FuncionarioRequest.Origem4 montarOrigem4(FuncionarioImportRow row) {
        FuncionarioRequest.Origem4 o = new FuncionarioRequest.Origem4();
        o.setCodOrg4(!StringUtils.vazio(row.getCodOrg4()) ? row.getCodOrg4().trim() : "000001");
        return o;
    }

    private FuncionarioRequest.Origem5 montarOrigem5(FuncionarioImportRow row) {
        FuncionarioRequest.Origem5 o = new FuncionarioRequest.Origem5();
        o.setCodOrg5(!StringUtils.vazio(row.getCodOrg5()) ? row.getCodOrg5().trim() : "000001");
        o.setDescOrg5(!StringUtils.vazio(row.getDescOrg5()) ? row.getDescOrg5().trim() : "ORGÃO PRIVADO B.UNI");
        return o;
    }

    // ─── Resolvers ───────────────────────────────────────────────────────────────

    private String resolverCpf(String cpf) {
        String limpo = StringUtils.limparNumero(cpf);
        if (StringUtils.vazio(limpo)) return limpo;
        if (limpo.length() < 11) limpo = String.format("%11s", limpo).replace(' ', '0');
        if (limpo.length() > 11) limpo = limpo.substring(0, 11);
        return limpo;
    }

    private String resolverDataCadastro(FuncionarioImportRow row, String cpf) {
        if (!StringUtils.vazio(row.getDataCadastro())) {
            return DateUtils.formatarDataFuncionario(row.getDataCadastro());
        }
        registrarObservacao(row, "DATACADASTRO nao informada, preenchida com data atual");
        return DateUtils.agoraIso();
    }

    private String resolverTipoDocumento(String tipo) {
        return !StringUtils.vazio(tipo) ? tipo.trim() : "1";
    }

    private String resolverNumeroDocumento(String rg, String cpf) {
        return !StringUtils.vazio(rg) ? rg.trim() : cpf;
    }

    private String resolverOrgaoEmissor(String orgao) {
        return !StringUtils.vazio(orgao) ? orgao.trim().toUpperCase() : "SSP";
    }

    private String resolverSexo(String sexo) {
        if (StringUtils.vazio(sexo)) return "M";
        return switch (sexo.trim().toUpperCase()) {
            case "F", "2", "FEM", "FEMININO"     -> "F";
            case "M", "1", "MAS", "MASCULINO"    -> "M";
            default -> "M";
        };
    }

    private String resolverEstadoCivil(String ec) {
        if (StringUtils.vazio(ec)) return "1";
        return switch (ec.trim().toUpperCase()) {
            case "S", "SOLTEIRO", "SOLTEIRA"     -> "1";
            case "C", "CASADO", "CASADA"         -> "2";
            case "D", "DIVORCIADO", "DIVORCIADA" -> "3";
            case "V", "VIUVO", "VIUVA"           -> "4";
            case "SE", "SEPARADO", "SEPARADA"    -> "5";
            case "1", "2", "3", "4", "5"         -> ec.trim();
            default -> "1";
        };
    }

    private String resolverNacionalidade(String nac) {
        return !StringUtils.vazio(nac) ? limpar(nac) : "Brasileira";
    }

    private String resolverCodNacionalidade(String cod) {
        if (StringUtils.vazio(cod)) return "01";
        String limpo = cod.trim();
        try {
            return String.format("%02d", Integer.parseInt(limpo));
        } catch (NumberFormatException e) {
            return "01";
        }
    }

    private String resolverUf(String uf) {
        if (StringUtils.vazio(uf)) return "";
        String normalizado = uf.trim().toUpperCase();
        return UFS_VALIDAS.contains(normalizado) ? normalizado : "";
    }

    private String resolverEmail(String email, String cpf) {
        if (!StringUtils.vazio(email) && email.contains("@")) {
            return email.trim().toLowerCase();
        }
        return cpf + "@funcionario.local";
    }

    private String resolverCep(String cep) {
        if (StringUtils.vazio(cep)) return "";
        String limpo = StringUtils.limparNumero(cep);
        if (limpo.length() == 8) return limpo;
        if (limpo.length() > 8)  return limpo.substring(0, 8);
        return limpo;
    }

    private Integer resolverNumeroEndereco(String numero) {
        if (StringUtils.vazio(numero)) return 0;
        String limpo = numero.trim().replaceAll("[^0-9]", "");
        if (limpo.isBlank()) return 0;
        try {
            return Integer.parseInt(limpo.length() > 6 ? limpo.substring(0, 6) : limpo);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String resolverSituacao(String situacao) {
        if (StringUtils.vazio(situacao)) return "AT";
        return switch (situacao.trim().toUpperCase()) {
            case "ATIVO", "AT"        -> "AT";
            case "AFASTADO", "AF"     -> "AF";
            case "DESLIGADO", "DE"    -> "DE";
            default -> situacao.trim().toUpperCase();
        };
    }

    private String resolverTpVinculo(String tpVinculo) {
        return !StringUtils.vazio(tpVinculo) ? tpVinculo.trim() : "1";
    }

    private String resolverCodBanco(String banco) {
        String limpo = StringUtils.limparNumero(banco);
        if (!StringUtils.vazio(limpo)) {
            try {
                return String.format("%03d", Integer.parseInt(limpo));
            } catch (NumberFormatException e) {
                return limpo;
            }
        }
        return "530";
    }

    private String resolverTipoConta(String tipo) {
        if (StringUtils.vazio(tipo)) return "01";
        return switch (tipo.trim()) {
            case "1"                       -> "01";
            case "2"                       -> "02";
            case "01", "02", "11", "12"   -> tipo.trim();
            default                        -> "01";
        };
    }

    private String resolverAgencia(String agencia) {
        if (StringUtils.vazio(agencia)) return "0001";
        String limpo = agencia.trim().replaceAll("[^0-9]", "");
        if (limpo.isBlank()) return "0001";
        if (limpo.length() > 4) limpo = limpo.substring(limpo.length() - 4);
        return String.format("%4s", limpo).replace(' ', '0');
    }

    private String resolverConta(String conta) {
        if (StringUtils.vazio(conta)) return "0000000001";
        String limpo = conta.trim().replaceAll("[^0-9]", "");
        if (limpo.isBlank()) return "0000000001";
        if (limpo.length() > 17) limpo = limpo.substring(limpo.length() - 17);
        if (limpo.length() < 10) return String.format("%10s", limpo).replace(' ', '0');
        return limpo;
    }

    private String resolverDigitoConta(String digito) {
        if (StringUtils.vazio(digito)) return "0";
        String limpo = digito.trim().replaceAll("[^0-9]", "");
        return limpo.isBlank() ? "0" : limpo;
    }

    private double resolverDouble(String valor) {
        if (StringUtils.vazio(valor)) return 0.0;
        try {
            return Double.parseDouble(
                valor.trim()
                     .replace("R$", "")
                     .replace(" ", "")
                     .replace(".", "")
                     .replace(",", ".")
            );
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String resolverDataOpcional(String data) {
        if (StringUtils.vazio(data)) return null;
        String resultado = DateUtils.formatarDataFuncionario(data);
        if (resultado.isEmpty() || resultado.equals(data.trim())) return null;
        return resultado;
    }

    private String limpar(String valor) {
        if (StringUtils.vazio(valor)) return "";
        return valor.trim();
    }

    private String truncar(String valor, int maxLen) {
        if (valor == null) return "";
        return valor.length() > maxLen ? valor.substring(0, maxLen) : valor;
    }

    private void registrarObservacao(FuncionarioImportRow row, String nota) {
        String atual = row.getObservacao();
        if (StringUtils.vazio(atual)) {
            row.setObservacao(nota);
        } else if (!atual.contains(nota)) {
            row.setObservacao(atual + " | " + nota);
        }
    }
}
