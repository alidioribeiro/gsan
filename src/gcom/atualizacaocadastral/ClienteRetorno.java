
package gcom.atualizacaocadastral;

import java.util.Date;
import java.util.Set;

import gcom.cadastro.cliente.Cliente;
import gcom.cadastro.cliente.ClienteEndereco;
import gcom.cadastro.cliente.ClienteFone;
import gcom.cadastro.cliente.ClienteImovel;
import gcom.cadastro.cliente.ClienteTipo;
import gcom.cadastro.cliente.PessoaSexo;
import gcom.cadastro.geografico.UnidadeFederacao;

public class ClienteRetorno implements ICliente {
    
	private Integer id;
	private Integer idCliente;
	private String nome;
	private String rg;
	private String cpf;
	private String cnpj;
	private String email;
	private Date ultimaAlteracao;
	private Integer idClienteTipo;
	private Integer tipoOperacao;
	private Short indicadorTransmissaoCpfCnpj;
	
	private UnidadeFederacao unidadeFederacao;
	private PessoaSexo pessoaSexo;
	private ClienteTipo clienteTipo;
	private Cliente cliente;
	
	private Set<ClienteFone> clienteFones;
    private Set<ClienteImovel> clienteImoveis;
    private Set<ClienteEndereco> clienteEnderecos;
	private String numeroNIS;
	
	public ClienteRetorno() {
		
	}
	
	public ClienteRetorno(ICliente cliente) {
		this.idCliente = cliente.getIdCliente();
		this.nome = cliente.getNome();
		this.rg = cliente.getRg();
		this.cpf = cliente.getCpf();
		this.cnpj = cliente.getCnpj();
		this.email = cliente.getEmail();
		this.unidadeFederacao = cliente.getUnidadeFederacao();
		this.pessoaSexo = cliente.getPessoaSexo();
		this.tipoOperacao = cliente.getTipoOperacao();
		this.clienteTipo = new ClienteTipo(cliente.getIdClienteTipo());
		this.cliente = new Cliente(cliente.getIdCliente());
	}
	
	public Integer getIdCliente() {
		return idCliente;
	}

	public void setIdCliente(Integer idCliente) {
		this.idCliente = idCliente;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nomeCliente) {
		this.nome = nomeCliente;
	}

	public String getRg() {
		return rg;
	}

	public void setRg(String rg) {
		this.rg = rg;
	}

	public UnidadeFederacao getUnidadeFederacao() {
		return unidadeFederacao;
	}

	public void setUnidadeFederacao(UnidadeFederacao unidadeFederacao) {
		this.unidadeFederacao = unidadeFederacao;
	}

	public PessoaSexo getPessoaSexo() {
		return pessoaSexo;
	}

	public void setPessoaSexo(PessoaSexo pessoaSexo) {
		this.pessoaSexo = pessoaSexo;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getCnpj() {
		return cnpj;
	}

	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email =  email;
	}

	public Date getUltimaAlteracao() {
		return ultimaAlteracao;
	}

	public void setUltimaAlteracao(Date ultimaAlteracao) {
		this.ultimaAlteracao = ultimaAlteracao;
	}

	public ClienteTipo getClienteTipo() {
		return clienteTipo;
	}

	public void setClienteTipo(ClienteTipo clienteTipo) {
		this.clienteTipo = clienteTipo;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<ClienteFone> getClienteFones() {
		return clienteFones;
	}

	public void setClienteFones(Set<ClienteFone> clienteFones) {
		this.clienteFones = clienteFones;
	}

	public Set<ClienteImovel> getClienteImoveis() {
		return clienteImoveis;
	}

	public void setClienteImoveis(Set<ClienteImovel> clienteImoveis) {
		this.clienteImoveis = clienteImoveis;
	}

	public Set<ClienteEndereco> getClienteEnderecos() {
		return clienteEnderecos;
	}

	public void setClienteEnderecos(Set<ClienteEndereco> clienteEnderecos) {
		this.clienteEnderecos = clienteEnderecos;
	}

	public Integer getIdClienteTipo() {
		return idClienteTipo;
	}

	public void setIdClienteTipo(Integer idCLienteTipo) {
		this.idClienteTipo = idCLienteTipo;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Integer getTipoOperacao() {
		return tipoOperacao;
	}

	public void setTipoOperacao(Integer tipoOperacao) {
		this.tipoOperacao = tipoOperacao;
	}

	public Short getIndicadorTransmissaoCpfCnpj() {
		return indicadorTransmissaoCpfCnpj;
	}

	public void setIndicadorTransmissaoCpfCnpj(Short indicadorTransmissaoCpfCnpj) {
		this.indicadorTransmissaoCpfCnpj = indicadorTransmissaoCpfCnpj;
	}

	public String getNumeroNIS() {
		return this.numeroNIS;
	}

	public void setNumeroNIS(String numeroNIS) {
		this.numeroNIS = numeroNIS;
	}
}
