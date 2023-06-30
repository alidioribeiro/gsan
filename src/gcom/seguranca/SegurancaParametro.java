package gcom.seguranca;

import java.io.Serializable;

public class SegurancaParametro implements Serializable {

	private static final long serialVersionUID = -7918264574580624530L;

	private Integer id;
	private String nome;
	private String valor;

	public enum NOME_PARAMETRO_SEGURANCA {
		CAMINHO_ARQUIVOS,
		CAMINHO_LOG_BATCH,
		IP_NOVO_BATCH,
		URL_GSAN_RELATORIOS,
		URL_SEGURANCA,
		URL_SEGUNDA_VIA,
		URL_CONTRATO,
		URL_AVISO_CORTE,
		VERSAO_APLICATIVO_RECADASTRAMENTO,
		URL_SERVIDOR_IMAGENS,
		URL_ORDEM_SUSPENSAO_FORNECIMENTO,
		URL_API_REGISTRAR_BOLETO_BB,
		URL_REGISTRO_BB_TESTE,
		URL_ENTRADA_PARCELAMENTO,
		URL_ARQUIVOS_SERVER,
		URL_BASIC_AUTH
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}
}
