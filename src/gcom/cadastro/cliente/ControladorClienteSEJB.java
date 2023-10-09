package gcom.cadastro.cliente;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;

import gcom.cadastro.cliente.bean.ClienteEmitirBoletimCadastroHelper;
import gcom.cadastro.cliente.bean.PesquisarClienteResponsavelSuperiorHelper;
import gcom.cadastro.endereco.Cep;
import gcom.cadastro.endereco.EnderecoReferencia;
import gcom.cadastro.endereco.EnderecoTipo;
import gcom.cadastro.endereco.Logradouro;
import gcom.cadastro.endereco.LogradouroBairro;
import gcom.cadastro.endereco.LogradouroCep;
import gcom.cadastro.endereco.LogradouroTipo;
import gcom.cadastro.endereco.LogradouroTitulo;
import gcom.cadastro.geografico.Bairro;
import gcom.cadastro.geografico.Municipio;
import gcom.cadastro.geografico.UnidadeFederacao;
import gcom.cadastro.localidade.Localidade;
import gcom.cadastro.sistemaparametro.SistemaParametro;
import gcom.cadastro.tarifasocial.FiltroTarifaSocialDadoEconomia;
import gcom.cadastro.tarifasocial.TarifaSocialDadoEconomia;
import gcom.interceptor.RegistradorOperacao;
import gcom.seguranca.AtributoGrupo;
import gcom.seguranca.acesso.Operacao;
import gcom.seguranca.acesso.usuario.Usuario;
import gcom.seguranca.acesso.usuario.UsuarioAcao;
import gcom.seguranca.acesso.usuario.UsuarioAcaoUsuarioHelper;
import gcom.util.ConstantesSistema;
import gcom.util.ControladorComum;
import gcom.util.ControladorException;
import gcom.util.ErroRepositorioException;
import gcom.util.Util;
import gcom.util.filtro.ParametroSimples;
import gcom.util.filtro.ParametroSimplesDiferenteDe;

public class ControladorClienteSEJB extends ControladorComum {

	private static final long serialVersionUID = 1L;

	private IRepositorioCliente repositorioCliente = null;

	private IRepositorioClienteImovel repositorioClienteImovel = null;

	private IRepositorioClienteEndereco repositorioClienteEndereco = null;

	public void ejbCreate() throws CreateException {
		repositorioCliente = RepositorioClienteHBM.getInstancia();
		repositorioClienteImovel = RepositorioClienteImovelHBM.getInstancia();
		repositorioClienteEndereco = RepositorioClienteEnderecoHBM.getInstancia();

	}

	@SuppressWarnings("rawtypes")
	public Integer inserirCliente(Cliente cliente, Collection telefones, Collection enderecos, Usuario usuario) throws ControladorException {
		FiltroCliente filtroCliente = new FiltroCliente();

		// ------------ REGISTRAR TRANSA��O ----------------
		RegistradorOperacao registradorOperacao = new RegistradorOperacao(Operacao.OPERACAO_CLIENTE_INSERIR, cliente.getId(), cliente.getId(),
				new UsuarioAcaoUsuarioHelper(usuario, UsuarioAcao.USUARIO_ACAO_EFETUOU_OPERACAO));
		// ------------ REGISTRAR TRANSA��O ----------------

		// Validar CPF de cliente
		if (cliente.getCpf() != null && !cliente.getCpf().equals("")) {
			filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.CPF, cliente.getCpf()));

			Collection clienteComCpfExistente = getControladorUtil().pesquisar(filtroCliente, Cliente.class.getName());

			if (!clienteComCpfExistente.isEmpty()) {
				Cliente clienteComCpf = (Cliente) clienteComCpfExistente.iterator().next();
				sessionContext.setRollbackOnly();
				throw new ControladorException("atencao.cpf.cliente.ja_cadastrado", null, "" + clienteComCpf.getId());

			}
		}

		// Validar CNPJ de cliente
		if (cliente.getCnpj() != null && !cliente.getCnpj().equals("")) {
			filtroCliente.limparListaParametros();
			filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.CNPJ, cliente.getCnpj()));

			Collection clienteComCnpjExistente = getControladorUtil().pesquisar(filtroCliente, Cliente.class.getName());

			if (!clienteComCnpjExistente.isEmpty()) {
				Cliente clienteComCnpj = (Cliente) clienteComCnpjExistente.iterator().next();
				sessionContext.setRollbackOnly();
				throw new ControladorException("atencao.cnpj.cliente.ja_cadastrado", null, "" + clienteComCnpj.getId());
			}
		}

		// Validar RG de cliente
		if (cliente.getRg() != null && !cliente.getRg().equals("")) {
			filtroCliente.limparListaParametros();
			filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.RG, cliente.getRg()));
			filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.ORGAO_EXPEDIDOR_RG, cliente.getOrgaoExpedidorRg()));
			filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.UNIDADE_FEDERACAO, cliente.getUnidadeFederacao()));

			Collection clienteComRgExistente = getControladorUtil().pesquisar(filtroCliente, Cliente.class.getName());

			if (!clienteComRgExistente.isEmpty()) {
				Cliente clienteComRg = (Cliente) clienteComRgExistente.iterator().next();
				sessionContext.setRollbackOnly();
				throw new ControladorException("atencao.rg.cliente.ja_cadastrado", null, "" + clienteComRg.getId());
			}
		}

		// -------------Parte que insere um cliente na
		// base----------------------
		cliente.setIndicadorGeraArquivoTexto(new Short("2"));

		// *************************************************************************
		// Autor: Ivan Sergio
		// Data: 06/08/2009
		// CRC2103
		// Caso em que o Inserir Cliente eh chamdo como PopUp pelo Manter Imovel
		// *************************************************************************
		AtributoGrupo atributoGrupo = null;
		if (cliente.getOperacaoEfetuada() != null) {
			if (cliente.getOperacaoEfetuada().getAtributoGrupo() != null) {
				atributoGrupo = cliente.getOperacaoEfetuada().getAtributoGrupo();
			}
		}

		registradorOperacao.registrarOperacao(cliente);

		if (atributoGrupo != null) {
			cliente.getOperacaoEfetuada().setAtributoGrupo(atributoGrupo);
		}
		// *************************************************************************

		cliente.setIndicadorAcaoCobranca(ConstantesSistema.INDICADOR_USO_ATIVO);

		Integer chaveClienteGerada = (Integer) getControladorUtil().inserir(cliente);

		cliente.setId(chaveClienteGerada);

		if (telefones != null) {
			Iterator iteratorTelefones = telefones.iterator();

			while (iteratorTelefones.hasNext()) {
				ClienteFone clienteFone = (ClienteFone) iteratorTelefones.next();

				clienteFone.setCliente(cliente);
				registradorOperacao.registrarOperacao(clienteFone);
				getControladorUtil().inserir(clienteFone);

			}
		}

		// Inserir os endere�os do cliente
		Iterator iteratorEnderecos = enderecos.iterator();

		while (iteratorEnderecos.hasNext()) {
			ClienteEndereco clienteEndereco = (ClienteEndereco) iteratorEnderecos.next();

			clienteEndereco.setCliente(cliente);
			registradorOperacao.registrarOperacao(clienteEndereco);
			getControladorUtil().inserir(clienteEndereco);
		}

		return chaveClienteGerada;
	}

	@SuppressWarnings("rawtypes")
	public void atualizarCliente(Cliente cliente, Collection telefones, Collection enderecos, Usuario usuario) throws ControladorException {
		if (telefones == null) {
			telefones = new ArrayList<ClienteFone>();
		}

		if (enderecos == null) {
			enderecos = new ArrayList<ClienteEndereco>();
		}

		RegistradorOperacao registradorOperacao = new RegistradorOperacao(Operacao.OPERACAO_CLIENTE_ATUALIZAR, cliente.getId(), cliente.getId(),
				new UsuarioAcaoUsuarioHelper(usuario, UsuarioAcao.USUARIO_ACAO_EFETUOU_OPERACAO));

		FiltroCliente filtroCliente = new FiltroCliente();

		try {
			if (cliente.getCnpj() != null) {
				filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.CNPJ, cliente.getCnpj()));
				filtroCliente.adicionarParametro(new ParametroSimplesDiferenteDe(FiltroCliente.ID, cliente.getId()));

				Collection colecaoClientes = getControladorUtil().pesquisar(filtroCliente, Cliente.class.getName());

				if (colecaoClientes != null && !colecaoClientes.isEmpty()) {
					Cliente clienteComCnpj = (Cliente) Util.retonarObjetoDeColecao(colecaoClientes);
					sessionContext.setRollbackOnly();
					throw new ControladorException("atencao.cnpj.cliente.ja_cadastrado", null, "" + clienteComCnpj.getId());
				}
			}

			// Parte de Validacao com Timestamp
			filtroCliente.limparListaParametros();

			// Seta o filtro para buscar o cliente na base
			filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.ID, cliente.getId()));

			Collection colecaoCliente = getControladorUtil().pesquisar(filtroCliente, Cliente.class.getName());

			// verifica se o cliente ainda existe na base, porque ele pode ter
			// sido excluido com isso
			// n�o � poss�vel analizar a data de ultima altera��o
			if (colecaoCliente == null || colecaoCliente.isEmpty()) {
				sessionContext.setRollbackOnly();
				throw new ControladorException("atencao.atualizacao.timestamp");
			}

			// Procura o cliente na base
			Cliente clienteNaBase = (Cliente) ((List) colecaoCliente).get(0);

			// Verificar se o cliente j� foi atualizado por outro usu�rio
			// durante esta atualiza��o
			if (clienteNaBase.getUltimaAlteracao().after(cliente.getUltimaAlteracao())) {
				sessionContext.setRollbackOnly();
				throw new ControladorException("atencao.atualizacao.timestamp");
			}

			// Altualiza o indicadorGeraArquivotexto
			cliente.setIndicadorGeraArquivoTexto(clienteNaBase.getIndicadorGeraArquivoTexto());
			// Atualiza a data de �ltima altera��o
			cliente.setUltimaAlteracao(new Date());

			// *************************************************************************
			// Autor: Ivan Sergio
			// Data: 06/08/2009
			// CRC2103
			// Caso em que o Inserir Cliente eh chamdo como PopUp pelo Manter
			// Imovel
			// *************************************************************************
			AtributoGrupo atributoGrupo = null;
			if (cliente.getOperacaoEfetuada() != null) {
				if (cliente.getOperacaoEfetuada().getAtributoGrupo() != null) {
					atributoGrupo = cliente.getOperacaoEfetuada().getAtributoGrupo();
				}
			}

			registradorOperacao.registrarOperacao(cliente);

			if (atributoGrupo != null) {
				cliente.getOperacaoEfetuada().setAtributoGrupo(atributoGrupo);
			}
			// *************************************************************************

			cliente.setClienteFones(new HashSet(telefones));
			cliente.setClienteEnderecos(new HashSet(enderecos));

			// Atualiza o cliente
			cliente.setUsuarioParaHistorico(usuario);
			getControladorAtualizacaoCadastro().atualizar(cliente);

			// Atualizar os fones do cliente
			Iterator iteratorTelefones = telefones.iterator();

			// Remover a lista dos telefones do cliente para adicionar a nova
			// lista depois
			repositorioCliente.removerTodosTelefonesPorCliente(cliente.getId());

			// Adicionar os telefones novos informados para o cliente
			while (iteratorTelefones.hasNext()) {
				ClienteFone clienteFone = (ClienteFone) iteratorTelefones.next();

				clienteFone.setUltimaAlteracao(new Date());
				clienteFone.setCliente(cliente);
				getControladorUtil().inserir(clienteFone);
			}

			// Inserir os endere�os do cliente
			Iterator iteratorEnderecos = enderecos.iterator();

			// Remover a lista dos endere�os do cliente para adicionar a nova
			// lista depois
			repositorioCliente.removerTodosEnderecosPorCliente(cliente.getId());

			// Adicionar os endere�os novos informados para o cliente
			while (iteratorEnderecos.hasNext()) {
				ClienteEndereco clienteEndereco = (ClienteEndereco) iteratorEnderecos.next();

				clienteEndereco.setUltimaAlteracao(new Date());
				clienteEndereco.setCliente(cliente);
				getControladorUtil().inserir(clienteEndereco);
			}

		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	/**
	 * < <Descri��o do m�todo>>
	 * 
	 * @param clienteImovel
	 *            Descri��o do par�metro
	 * @throws ControladorException
	 */
	public void inserirClienteImovel(ClienteImovel clienteImovel) throws ControladorException {

		clienteImovel.setDataInicioRelacao(new Date());

		getControladorUtil().inserir(clienteImovel);
	}

	/**
	 * Pesquisa uma cole��o de cliente imovel com uma query especifica
	 * 
	 * @param filtroClienteImovel
	 *            parametros para a consulta
	 * @return Description of the Return Value
	 * @throws ControladorException
	 */
	@SuppressWarnings("rawtypes")
	public Collection pesquisarClienteImovel(FiltroClienteImovel filtroClienteImovel, Integer numeroPagina) throws ControladorException {
		try {
			return repositorioClienteImovel.pesquisarClienteImovel(filtroClienteImovel, numeroPagina);
		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * Pesquisa uma a quantidade de cliente imovel com uma query especifica
	 * [UC0015] Filtrar Imovel
	 * 
	 * @param filtroClienteImovel
	 *            parametros para a consulta
	 * @author Rafael Santos
	 * @since 26/06/2006
	 * 
	 * @return Description of the Return Value
	 * @throws ControladorException
	 */
	public int pesquisarQuantidadeClienteImovel(FiltroClienteImovel filtroClienteImovel) throws ControladorException {
		try {
			return repositorioClienteImovel.pesquisarQuantidadeClienteImovel(filtroClienteImovel);
		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * Pesquisa uma cole��o de cliente imovel com uma query especifica
	 * 
	 * @param filtroClienteImovel
	 *            parametros para a consulta
	 * @return Description of the Return Value
	 * @throws ControladorException
	 */
	@SuppressWarnings("rawtypes")
	public Collection pesquisarClienteImovelTarifaSocial(FiltroClienteImovel filtroClienteImovel, Integer numeroPagina) throws ControladorException {
		try {
			return repositorioClienteImovel.pesquisarClienteImovel(filtroClienteImovel, numeroPagina);
		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	@SuppressWarnings("rawtypes")
	public Collection pesquisarCliente(FiltroCliente filtroCliente) throws ControladorException {
		Collection coll = getControladorUtil().pesquisar(filtroCliente, Cliente.class.getSimpleName());
		return coll;
	}

	@SuppressWarnings("rawtypes")
	public Collection pesquisarClienteEndereco(FiltroClienteEndereco filtroClienteEndereco) throws ControladorException {
		try {
			return repositorioClienteEndereco.pesquisarClienteEndereco(filtroClienteEndereco);
		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	/**
	 * atualiza um cliente imovel economia com a data final da rela��o e o
	 * motivo.
	 * 
	 * @param clientesImoveisEconomia
	 *            Description of the Parameter
	 * @throws ControladorException
	 */
	public void atualizarClienteImovelEconomia(Collection clientesImoveisEconomia) throws ControladorException {

		// try {
		// -------------Parte que atualiza um cliente imovel economia na
		// base---------------------

		Iterator clienteImovelEconomiaIterator = clientesImoveisEconomia.iterator();

		while (clienteImovelEconomiaIterator.hasNext()) {
			ClienteImovelEconomia clienteImovelEconomia = (ClienteImovelEconomia) clienteImovelEconomiaIterator.next();

			// Parte de Validacao com Timestamp
			FiltroClienteImovelEconomia filtroClienteImovelEconomia = new FiltroClienteImovelEconomia();

			// Seta o filtro para buscar o cliente imovel economia na base
			filtroClienteImovelEconomia.adicionarParametro(new ParametroSimples(FiltroClienteImovelEconomia.ID, clienteImovelEconomia.getId()));

			// Procura o cliente na base
			ClienteImovelEconomia clienteImovelEconomiaNaBase = (ClienteImovelEconomia) ((List) (getControladorUtil().pesquisar(filtroClienteImovelEconomia,
					ClienteImovelEconomia.class.getName()))).get(0);

			// Verificar se o cliente j� foi atualizado por outro usu�rio
			// durante
			// esta atualiza��o
			if (clienteImovelEconomiaNaBase.getUltimaAlteracao().after(clienteImovelEconomia.getUltimaAlteracao())) {
				sessionContext.setRollbackOnly();
				throw new ControladorException("atencao.atualizacao.timestamp");
			}

			// Atualiza a data de �ltima altera��o
			clienteImovelEconomia.setUltimaAlteracao(new Date());

			// Atualiza o cliente
			getControladorUtil().atualizar(clienteImovelEconomia);
		}
		/*
		 * } catch (ErroRepositorioException ex) {
		 * sessionContext.setRollbackOnly(); throw new
		 * ControladorException("erro.sistema", ex); }
		 */

	}

	/**
	 * Pesquisa ClienteEndereco percorrendo o ClienteImovel
	 * 
	 * @param filtroClienteEndereco
	 * @return
	 * @throws ControladorException
	 */
	public Collection<Cliente> pesquisarClienteEnderecoClienteImovel(FiltroClienteEndereco filtroClienteEndereco) throws ControladorException {
		try {
			return repositorioClienteEndereco.pesquisarClienteEnderecoClienteImovel(filtroClienteEndereco);

		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public Collection<Cliente> pesquisarClienteDadosClienteEndereco(FiltroCliente filtroCliente, Integer numeroPagina) throws ControladorException {

		try {
			return repositorioCliente.pesquisarClienteDadosClienteEndereco(filtroCliente, numeroPagina);

		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public Collection<Cliente> pesquisarClienteDadosClienteEnderecoRelatorio(FiltroCliente filtroCliente) throws ControladorException {

		try {
			return repositorioCliente.pesquisarClienteDadosClienteEnderecoRelatorio(filtroCliente);

		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * <Breve descri��o sobre o caso de uso>
	 * 
	 * <Identificador e nome do caso de uso>
	 * 
	 * Retrona a quantidade de endere�os que existem para o Cliente
	 * 
	 * pesquisarClienteDadosClienteEnderecoCount
	 * 
	 * @author Roberta Costa
	 * @date 29/06/2006
	 * 
	 * @param filtroCliente
	 * @return
	 * @throws ControladorException
	 */
	public Integer pesquisarClienteDadosClienteEnderecoCount(FiltroCliente filtroCliente) throws ControladorException {

		try {
			return repositorioCliente.pesquisarClienteDadosClienteEnderecoCount(filtroCliente);

		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public Collection<Cliente> pesquisarClienteOutrosCriterios(FiltroCliente filtroCliente) throws ControladorException {

		try {
			return repositorioCliente.pesquisarClienteOutrosCriterios(filtroCliente);

		} catch (ErroRepositorioException ex) {
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * Inseri uma cole��o de pagamentos no sistema
	 * 
	 * [UC0265] Inserir Pagamentos
	 * 
	 * Este fluxo secund�rio tem como objetivo pesquisar o cliente digitado pelo
	 * usu�rio
	 * 
	 * [FS0011] - Verificar exist�ncia do c�digo do cliente
	 * 
	 * @author Pedro Alexandre
	 * @date 16/02/2006
	 * 
	 * @param idClienteDigitado
	 * @return
	 * @throws ControladorException
	 */
	public Cliente pesquisarClienteDigitado(Integer idClienteDigitado) throws ControladorException {

		// Cria a vari�vel que vai armazenar o cliente pesquisado
		Cliente clienteDigitado = null;

		// Pesquisa o cliente informado pelo usu�rio no sistema
		FiltroCliente filtroCliente = new FiltroCliente();
		filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.ID, idClienteDigitado));

		filtroCliente.adicionarCaminhoParaCarregamentoEntidade(FiltroCliente.CLIENTE_TIPO);
		filtroCliente.adicionarCaminhoParaCarregamentoEntidade(FiltroCliente.ESFERA_PODER);

		Collection<Cliente> colecaoCliente = getControladorUtil().pesquisar(filtroCliente, Cliente.class.getName());

		// Caso exista o cliente no sistema
		// Retorna para o usu�rio o cliente retornado pela pesquisa
		// Caso contr�rio retorna um objeto nulo
		if (colecaoCliente != null && !colecaoCliente.isEmpty()) {
			clienteDigitado = (Cliente) Util.retonarObjetoDeColecao(colecaoCliente);
		}

		// Retorna o cliente encontrado ou nulo se n�o existir
		return clienteDigitado;
	}

	public Cliente pesquisarCliente(Integer idCliente) throws ControladorException {
		try {
			return repositorioCliente.pesquisarCliente(idCliente);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}
	}

	/**
	 * Pesquisa uma cole��o de cliente imovel com uma query especifica
	 * 
	 * @param filtroClienteImovel
	 *            parametros para a consulta
	 * @return Description of the Return Value
	 * @exception ErroRepositorioException
	 *                Description of the Exception
	 */

	public Collection pesquisarClienteImovel(FiltroClienteImovel filtroClienteImovel) throws ControladorException {
		// Retorna o cliente encontrado ou vazio se n�o existir
		try {
			return repositorioClienteImovel.pesquisarClienteImovel(filtroClienteImovel);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}

	}

	/**
	 * Pesquisa uma cole��o de cliente imovel com uma query especifica
	 * 
	 * @param filtroClienteImovel
	 *            parametros para a consulta
	 * @return Description of the Return Value
	 * @exception ErroRepositorioException
	 *                Description of the Exception
	 */

	public Collection pesquisarClienteImovelRelatorio(FiltroClienteImovel filtroClienteImovel) throws ControladorException {
		// Retorna o cliente encontrado ou vazio se n�o existir
		try {
			return repositorioClienteImovel.pesquisarClienteImovelRelatorio(filtroClienteImovel);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}

	}

	/**
	 * Pesquisa um cliente pelo id
	 * 
	 * @author Rafael Corr�a
	 * @date 01/08/2006
	 * 
	 * @return Cliente
	 * @exception ErroRepositorioException
	 *                Erro no hibernate
	 */
	public Cliente pesquisarObjetoClienteRelatorio(Integer idCliente) throws ControladorException {

		try {
			// pesquisa as gerencias regionais existentes no sisitema
			Object[] objetoCliente = repositorioCliente.pesquisarObjetoClienteRelatorio(idCliente);

			Cliente cliente = new Cliente();

			cliente.setId((Integer) objetoCliente[0]);
			cliente.setNome((String) objetoCliente[1]);

			return cliente;

			// erro no hibernate
		} catch (ErroRepositorioException ex) {

			// levanta a exce��o para a pr�xima camada
			throw new ControladorException("erro.sistema", ex);
		}
	}

	/**
	 * Pesquisa as quantidades de im�veis e as quantidades de economias
	 * associadas a um cliente
	 * 
	 * @author Rafael Corr�a
	 * @date 23/08/2007
	 * 
	 * @return Object[]
	 * @exception ControladorException
	 *                Erro no hibernate
	 */
	public Object[] pesquisarQtdeImoveisEEconomiasCliente(Integer idCliente) throws ControladorException {

		try {
			// pesquisa as gerencias regionais existentes no sisitema
			return repositorioCliente.pesquisarQtdeImoveisEEconomiasCliente(idCliente);

			// erro no hibernate
		} catch (ErroRepositorioException ex) {

			// levanta a exce��o para a pr�xima camada
			throw new ControladorException("erro.sistema", ex);
		}
	}

	/**
	 * O m�todo abaixo realiza uma pesquisa em cliente e verifica se ele existe
	 * 
	 */
	public Integer verificarExistenciaCliente(Integer codigoCliente) throws ControladorException {

		// Retorna o cliente encontrado ou vazio se n�o existir
		try {
			return repositorioCliente.verificarExistenciaCliente(codigoCliente);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}

	}

	/**
	 * [UC0366] Inserir Registro de Atendimento
	 * 
	 * Pesquisar ClienteImovel
	 * 
	 * @author Raphael Rossiter
	 * @date 21/08/2006
	 * 
	 * 
	 * @param idCliente
	 *            , idImovel
	 * @return Colletion
	 * @throws ControladorException
	 */
	public ClienteImovel pesquisarClienteImovel(Integer idCliente, Integer idImovel) throws ControladorException {

		Collection colecaoClienteImovel = null;

		ClienteImovel retorno = null;

		try {
			colecaoClienteImovel = this.repositorioClienteImovel.pesquisarClienteImovel(idCliente, idImovel);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

		if (colecaoClienteImovel != null && !colecaoClienteImovel.isEmpty()) {

			retorno = new ClienteImovel();
			Cliente cliente = new Cliente();

			Iterator raIterator = colecaoClienteImovel.iterator();

			Object[] arrayClienteIMovel = (Object[]) raIterator.next();

			cliente.setId((Integer) arrayClienteIMovel[0]);

			cliente.setNome((String) arrayClienteIMovel[1]);

			retorno.setCliente(cliente);
		}

		return retorno;
	}

	@SuppressWarnings("unchecked")
	public Collection<ClienteFone> pesquisarClienteFone(Integer idCliente) throws ControladorException {

		try {
			return this.repositorioCliente.pesquisarClienteFone(idCliente);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

	}

	@SuppressWarnings("unchecked")
	public Collection<ClienteFone> pesquisarClienteFoneDoImovel(Integer idImovel) throws ControladorException {

		try {
			return this.repositorioCliente.pesquisarClienteFoneDoImovel(idImovel);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * [UC0366] Inserir Registro de Atendimento
	 * 
	 * @author Raphael Rossiter
	 * @date 21/08/2006
	 * 
	 * @param idCliente
	 * @return Collection
	 * @throws ErroRepositorioException
	 */
	public Collection pesquisarEnderecosClienteAbreviado(Integer idCliente) throws ControladorException {

		Collection colecaoEndereco = null;
		Collection colecaoRetorno = null;

		try {
			colecaoEndereco = this.repositorioClienteEndereco.pesquisarEnderecosClienteAbreviado(idCliente);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

		if (colecaoEndereco != null && !colecaoEndereco.isEmpty()) {

			Iterator enderecoIterator = colecaoEndereco.iterator();
			ClienteEndereco clienteEndereco = null;

			colecaoRetorno = new ArrayList();

			while (enderecoIterator.hasNext()) {

				clienteEndereco = new ClienteEndereco();

				// cria um array de objetos para pegar os parametros de
				// retorno da pesquisa
				Object[] arrayEndereco = (Object[]) enderecoIterator.next();

				LogradouroCep logradouroCep = null;
				if (arrayEndereco[20] != null) {

					logradouroCep = new LogradouroCep();
					logradouroCep.setId((Integer) arrayEndereco[20]);

					// id do Logradouro
					Logradouro logradouro = null;
					if (arrayEndereco[19] != null) {
						logradouro = new Logradouro();
						logradouro.setId((Integer) arrayEndereco[19]);
						logradouro.setNome("" + arrayEndereco[0]);
					}
					LogradouroTipo logradouroTipo = null;
					// Descri��o logradouro tipo
					if (arrayEndereco[22] != null) {
						logradouroTipo = new LogradouroTipo();
						logradouroTipo.setDescricaoAbreviada("" + arrayEndereco[22]);
						logradouro.setLogradouroTipo(logradouroTipo);
					}
					LogradouroTitulo logradouroTitulo = null;
					// Descri��o logradouro titulo
					if (arrayEndereco[23] != null) {
						logradouroTitulo = new LogradouroTitulo();
						logradouroTitulo.setDescricaoAbreviada("" + arrayEndereco[23]);
						logradouro.setLogradouroTitulo(logradouroTitulo);
					}
					// id do CEP
					Cep cep = null;
					if (arrayEndereco[10] != null) {
						cep = new Cep();
						cep.setCepId((Integer) arrayEndereco[10]);
						cep.setCodigo((Integer) arrayEndereco[16]);
					}

					logradouroCep.setLogradouro(logradouro);
					logradouroCep.setCep(cep);
				}

				clienteEndereco.setLogradouroCep(logradouroCep);

				LogradouroBairro logradouroBairro = null;
				if (arrayEndereco[21] != null) {

					logradouroBairro = new LogradouroBairro();
					logradouroBairro.setId((Integer) arrayEndereco[21]);

					Bairro bairro = null;
					// nome bairro
					if (arrayEndereco[3] != null) {
						bairro = new Bairro();
						bairro.setId((Integer) arrayEndereco[3]);
						bairro.setNome("" + arrayEndereco[4]);
					}

					Municipio municipio = null;
					// nome municipio
					if (arrayEndereco[5] != null) {
						municipio = new Municipio();
						municipio.setId((Integer) arrayEndereco[5]);
						municipio.setNome("" + arrayEndereco[6]);

						// id da unidade federa��o
						if (arrayEndereco[7] != null) {
							UnidadeFederacao unidadeFederacao = new UnidadeFederacao();
							unidadeFederacao.setId((Integer) arrayEndereco[7]);
							unidadeFederacao.setSigla("" + arrayEndereco[8]);
							municipio.setUnidadeFederacao(unidadeFederacao);
						}

						bairro.setMunicipio(municipio);
					}

					logradouroBairro.setBairro(bairro);
				}

				clienteEndereco.setLogradouroBairro(logradouroBairro);

				// descricao de endere�o refer�ncia
				if (arrayEndereco[24] != null) {
					EnderecoReferencia enderecoReferencia = new EnderecoReferencia();
					enderecoReferencia.setDescricao("" + arrayEndereco[24]);
					clienteEndereco.setEnderecoReferencia(enderecoReferencia);
				}

				// numero imovel
				if (arrayEndereco[17] != null) {
					clienteEndereco.setNumero("" + arrayEndereco[17]);
				}

				// complemento endere�o
				if (arrayEndereco[18] != null) {
					clienteEndereco.setComplemento("" + arrayEndereco[18]);
				}

				// indicador endere�o correspondencia
				if (arrayEndereco[25] != null) {
					clienteEndereco.setIndicadorEnderecoCorrespondencia((Short) arrayEndereco[25]);
				}

				clienteEndereco.setId((Integer) arrayEndereco[26]);

				clienteEndereco.setUltimaAlteracao((Date) arrayEndereco[27]);

				// Per�metro
				if (arrayEndereco[28] != null) {
					Logradouro perimetroInicial = new Logradouro();
					perimetroInicial.setId((Integer) arrayEndereco[28]);

					if (arrayEndereco[29] != null) {
						perimetroInicial.setNome((String) arrayEndereco[29]);
					}

					if (arrayEndereco[30] != null) {
						LogradouroTipo logradouroTipo = new LogradouroTipo();
						logradouroTipo.setDescricaoAbreviada((String) arrayEndereco[30]);
						perimetroInicial.setLogradouroTipo(logradouroTipo);
					}

					if (arrayEndereco[31] != null) {
						LogradouroTitulo logradouroTitulo = new LogradouroTitulo();
						logradouroTitulo.setDescricaoAbreviada((String) arrayEndereco[31]);
						perimetroInicial.setLogradouroTitulo(logradouroTitulo);
					}

					clienteEndereco.setPerimetroInicial(perimetroInicial);
				}

				if (arrayEndereco[32] != null) {
					Logradouro perimetroFinal = new Logradouro();
					perimetroFinal.setId((Integer) arrayEndereco[32]);

					if (arrayEndereco[33] != null) {
						perimetroFinal.setNome((String) arrayEndereco[33]);
					}

					if (arrayEndereco[34] != null) {
						LogradouroTipo logradouroTipo = new LogradouroTipo();
						logradouroTipo.setDescricaoAbreviada((String) arrayEndereco[34]);
						perimetroFinal.setLogradouroTipo(logradouroTipo);
					}

					if (arrayEndereco[35] != null) {
						LogradouroTitulo logradouroTitulo = new LogradouroTitulo();
						logradouroTitulo.setDescricaoAbreviada((String) arrayEndereco[35]);
						perimetroFinal.setLogradouroTitulo(logradouroTitulo);
					}

					clienteEndereco.setPerimetroFinal(perimetroFinal);
				}

				colecaoRetorno.add(clienteEndereco);
			}
		}

		return colecaoRetorno;
	}

	public Cliente consultarCliente(Integer idCliente) throws ControladorException {

		Collection colecaoClientes = null;
		// Collection colecaoRetorno = null;
		Cliente cliente = null;

		try {
			colecaoClientes = this.repositorioCliente.consultarCliente(idCliente);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

		if (colecaoClientes != null && !colecaoClientes.isEmpty()) {

			Object[] objetoCliente = (Object[]) colecaoClientes.iterator().next();

			cliente = new Cliente();

			// 0 - Nome do Cliente
			if (objetoCliente[0] != null) {
				cliente.setNome((String) objetoCliente[0]);
				cliente.setId((Integer) objetoCliente[20]);
			}

			// 1 - Nome do Cliente Abreviado
			if (objetoCliente[1] != null) {
				cliente.setNomeAbreviado((String) objetoCliente[1]);
			}

			// 2 - Data Vencimento
			if (objetoCliente[2] != null) {
				cliente.setDataVencimento((Short) objetoCliente[2]);
			}

			// 3 - Cliente Tipo Descricao
			ClienteTipo clienteTipo = null;
			if (objetoCliente[3] != null) {
				clienteTipo = new ClienteTipo();
				clienteTipo.setDescricao((String) objetoCliente[3]);
				cliente.setClienteTipo(clienteTipo);
			}

			// 4 - Indicado Pessoa Fisica ou Juridica
			if (objetoCliente[4] != null) {
				if (clienteTipo == null) {
					clienteTipo = new ClienteTipo();
				}
				clienteTipo.setIndicadorPessoaFisicaJuridica((Short) objetoCliente[4]);
				cliente.setClienteTipo(clienteTipo);
			}
			/**
			 * @Data 23/10/2013
			 * @author adriana Muniz e Wellington Rocha Adi��o do id do cliente
			 *         tipo no objeto
			 * */
			if (objetoCliente[19] != null) {
				if (clienteTipo == null) {
					clienteTipo = new ClienteTipo();
				}
				clienteTipo.setId((Integer) objetoCliente[19]);
				cliente.setClienteTipo(clienteTipo);
			}

			// 5 - E-mail
			if (objetoCliente[5] != null) {
				cliente.setEmail((String) objetoCliente[5]);
			}

			// 6 - Indicador de Acao Cobranca
			if (objetoCliente[6] != null) {
				cliente.setIndicadorAcaoCobranca((Short) objetoCliente[6]);
			}

			// 7 - CPF
			if (objetoCliente[7] != null) {
				cliente.setCpf((String) objetoCliente[7]);
			}

			// 8 - RG
			if (objetoCliente[8] != null) {
				cliente.setRg((String) objetoCliente[8]);
			}

			// 9 - Data Emissao RG
			if (objetoCliente[9] != null) {
				cliente.setDataEmissaoRg((Date) objetoCliente[9]);
			}

			// 10 - Orgao Expedidor RG
			OrgaoExpedidorRg orgaoExpedidorRg = null;
			if (objetoCliente[10] != null) {
				orgaoExpedidorRg = new OrgaoExpedidorRg();
				orgaoExpedidorRg.setDescricaoAbreviada((String) objetoCliente[10]);
				cliente.setOrgaoExpedidorRg(orgaoExpedidorRg);
			}

			// 11 - Unidade Federacao
			UnidadeFederacao unidadeFederacao = null;
			if (objetoCliente[11] != null) {
				unidadeFederacao = new UnidadeFederacao();
				unidadeFederacao.setSigla((String) objetoCliente[11]);
				cliente.setUnidadeFederacao(unidadeFederacao);
			}

			// 12 - Data Nascimento
			if (objetoCliente[12] != null) {
				cliente.setDataNascimento((Date) objetoCliente[12]);
			}

			// 13 - Profissao
			Profissao profissao = null;
			if (objetoCliente[13] != null) {
				profissao = new Profissao();
				profissao.setDescricao((String) objetoCliente[13]);
				cliente.setProfissao(profissao);
			}

			// 14 - Pessoa Sexo
			PessoaSexo pessoaSexo = null;
			if (objetoCliente[14] != null) {
				pessoaSexo = new PessoaSexo();
				pessoaSexo.setDescricao((String) objetoCliente[14]);
				cliente.setPessoaSexo(pessoaSexo);
			}

			// 15 - CNPJ
			if (objetoCliente[15] != null) {
				cliente.setCnpj((String) objetoCliente[15]);
			}

			// 16 - Ramo Atividade
			RamoAtividade ramoAtividade = null;
			if (objetoCliente[16] != null) {
				ramoAtividade = new RamoAtividade();
				ramoAtividade.setDescricao((String) objetoCliente[16]);
				cliente.setRamoAtividade(ramoAtividade);
			}

			// 17 - Codigo Cliente Responsavel
			Cliente clienteResponsavel = null;
			if (objetoCliente[17] != null) {
				clienteResponsavel = new Cliente();
				clienteResponsavel.setId((Integer) objetoCliente[17]);
				cliente.setCliente(cliente);
			}

			// 18 - Nome Cliente Responsavel
			if (objetoCliente[17] != null) {
				if (clienteResponsavel == null) {
					clienteResponsavel = new Cliente();
				}
				clienteResponsavel.setNome((String) objetoCliente[18]);
				cliente.setCliente(cliente);
			}
			
			//21 - N�mero NIS
			if (objetoCliente[21] != null) {
				cliente.setNumeroNIS((String) objetoCliente[21]);
			}
			//22 - Recusa subsidio
			if (objetoCliente[22] != null) {
				cliente.setIndicadorRecusaSubsidio((Short) objetoCliente[22]);
			}

		}

		return cliente;
	}

	/**
	 * Pesquisa todos os endere�os do cliente
	 * 
	 * @author Rafael Santos
	 * @date 13/09/2006
	 * 
	 * @param idCliente
	 * @return Collection
	 * @throws ErroRepositorioException
	 */
	public Collection pesquisarEnderecoCliente(Integer idCliente) throws ControladorException {

		Collection colecaoClienteEndereco = null;
		Collection colecaoRetorno = null;

		try {
			colecaoClienteEndereco = this.repositorioCliente.pesquisarEnderecosCliente(idCliente);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

		if (colecaoClienteEndereco != null && !colecaoClienteEndereco.isEmpty()) {

			Iterator clienteEnderecoIterator = colecaoClienteEndereco.iterator();
			ClienteEndereco clienteEndereco = null;
			colecaoRetorno = new ArrayList();

			while (clienteEnderecoIterator.hasNext()) {

				Object[] array = (Object[]) clienteEnderecoIterator.next();

				clienteEndereco = new ClienteEndereco();

				// 0- Endereco Tipo
				EnderecoTipo enderecoTipo = null;
				if (array[0] != null) {
					enderecoTipo = new EnderecoTipo();
					enderecoTipo.setDescricao((String) array[0]);
					clienteEndereco.setEnderecoTipo(enderecoTipo);
				}

				// 1- Indicador Endereco Correspondencia
				if (array[0] != null) {
					clienteEndereco.setIndicadorEnderecoCorrespondencia((Short) array[1]);
				}
				colecaoRetorno.add(clienteEndereco);
			}
		}

		return colecaoRetorno;

	}
	
	public String obterEnderecoCorrespondencia(Integer idCliente) throws ControladorException {
		StringBuilder enderecoFormatado = new StringBuilder();
		String[] endereco = getControladorEndereco().pesquisarEnderecoClienteDividido(idCliente);
		
		enderecoFormatado.append(endereco[0]).append(", ") 						// 0 - Endere�o (Tipo + Titulo + Logradouro)
						 .append("n ").append(endereco[5]).append(", ") 		// 5 - numero
						 .append("Bairro ").append(endereco[3]).append(", ")	// 3 - bairro
						 .append("cidade ").append(endereco[1]) 				// 1 - municipio
						 .append("-").append(endereco[2]).append(", ")			// 2 - unidade federe��o
						 .append("CEP ").append(endereco[4]).append(".");		// 4 - CEP

		return enderecoFormatado.toString();
	}

	/**
	 * Pesquisa o nome do cliente a partir do im�vel Autor: S�vio Luiz Data:
	 * 21/12/2005
	 */
	public String pesquisarNomeClientePorImovel(Integer idImovel) throws ControladorException {

		String nomeCliente = "";

		try {
			nomeCliente = this.repositorioClienteImovel.pesquisarNomeClientePorImovel(idImovel);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

		return nomeCliente;
	}

	/**
	 * Pesquisa o nome, cnpj e id do cliente a partir do im�vel Autor: Rafael
	 * Corr�a Data: 25/09/2006
	 */
	public Cliente pesquisarDadosClienteRelatorioParcelamentoPorImovel(Integer idImovel) throws ControladorException {

		Cliente cliente = null;
		Object[] dadosCliente = null;

		try {
			dadosCliente = this.repositorioClienteImovel.pesquisarDadosClienteRelatorioParcelamentoPorImovel(idImovel);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

		if (dadosCliente != null) {

			cliente = new Cliente();

			if (dadosCliente[0] != null) {
				// Id
				cliente.setId((Integer) dadosCliente[0]);
			}

			if (dadosCliente[1] != null) {
				// Nome
				cliente.setNome((String) dadosCliente[1]);
			} else {
				cliente.setNome("");
			}

			if (dadosCliente[2] != null) {
				// CPF
				cliente.setCpf((String) dadosCliente[2]);
			}

			if (dadosCliente[3] != null) {
				// CNPJ
				cliente.setCnpj((String) dadosCliente[3]);
			}

			if (dadosCliente[4] != null) {
				// org�o expedidor
				OrgaoExpedidorRg orgaoExpedidorRg = new OrgaoExpedidorRg();
				orgaoExpedidorRg.setDescricao((String) dadosCliente[4]);
				cliente.setOrgaoExpedidorRg(orgaoExpedidorRg);
			}

			if (dadosCliente[5] != null) {
				// unidade federativa
				UnidadeFederacao unidadeFederacao = new UnidadeFederacao();
				unidadeFederacao.setSigla((String) dadosCliente[5]);
				cliente.setUnidadeFederacao(unidadeFederacao);
			}

			if (dadosCliente[6] != null) {
				// rg
				cliente.setRg((String) dadosCliente[6]);
			}

		}

		return cliente;
	}

	/**
	 * [UC0458] - Imprimir Ordem de Servi�o
	 * 
	 * Pesquisa o telefone principal do Cliente para o Relat�rio de OS
	 * 
	 * @author Rafael Corr�a
	 * @date 17/10/2006
	 * 
	 * 
	 * @param idRegistroAtendimento
	 * @throws ErroRepositorioException
	 */

	public String pesquisarClienteFonePrincipal(Integer idCliente) throws ControladorException {

		Object[] dadosClienteFone = null;
		String telefoneFormatado = "";

		try {

			dadosClienteFone = this.repositorioCliente.pesquisarClienteFonePrincipal(idCliente);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

		if (dadosClienteFone != null) {

			// DDD
			if (dadosClienteFone[0] != null) {
				telefoneFormatado = telefoneFormatado + "(" + dadosClienteFone[0].toString() + ")";
			}

			// N�mero do Telefone
			if (dadosClienteFone[1] != null) {
				telefoneFormatado = telefoneFormatado + dadosClienteFone[1];
			}

			// Ramal
			if (dadosClienteFone[2] != null) {
				telefoneFormatado = telefoneFormatado + "/" + dadosClienteFone[2];
			}

		}

		return telefoneFormatado;

	}

	/**
	 * [UC0582] - Emitir Boletim de Cadastro
	 * 
	 * Pesquisa os dados do cliente para a emiss�o do boletim
	 * 
	 * @author Rafael Corr�a
	 * @date 16/05/2007
	 * 
	 * @param idImovel
	 *            , clienteRelacaoTipo
	 * @throws ControladorException
	 */
	public ClienteEmitirBoletimCadastroHelper pesquisarClienteEmitirBoletimCadastro(Integer idImovel, Short clienteRelacaoTipo) throws ControladorException {

		Collection colecaoDadosCliente = null;
		ClienteEmitirBoletimCadastroHelper clienteEmitirBoletimCadastroHelper = null;

		try {

			colecaoDadosCliente = this.repositorioCliente.pesquisarClienteEmitirBoletimCadastro(idImovel, clienteRelacaoTipo);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

		if (colecaoDadosCliente != null && !colecaoDadosCliente.isEmpty()) {

			Iterator colecaoDadosClienteIterator = colecaoDadosCliente.iterator();

			boolean primeiroRegistro = true;

			Collection clientesFone = new ArrayList();

			while (colecaoDadosClienteIterator.hasNext()) {

				Object[] dadosCliente = (Object[]) colecaoDadosClienteIterator.next();

				if (primeiroRegistro) {

					clienteEmitirBoletimCadastroHelper = new ClienteEmitirBoletimCadastroHelper();
					Cliente cliente = new Cliente();
					ClienteEndereco clienteEndereco = new ClienteEndereco();
					LogradouroCep logradouroCep = new LogradouroCep();
					LogradouroBairro logradouroBairro = new LogradouroBairro();

					// Dados do Cliente
					// Id do Cliente
					if (dadosCliente[0] != null) { // 0
						cliente.setId((Integer) dadosCliente[0]);
					}

					// Nome do Cliente
					if (dadosCliente[1] != null) { // 1
						cliente.setNome((String) dadosCliente[1]);
					}

					// Tipo do Cliente
					if (dadosCliente[2] != null) { // 2
						ClienteTipo clienteTipo = new ClienteTipo();
						clienteTipo.setId((Integer) dadosCliente[2]);
						cliente.setClienteTipo(clienteTipo);
					}

					// CPF do Cliente
					if (dadosCliente[3] != null) { // 3
						cliente.setCpf((String) dadosCliente[3]);
					}

					// CNPJ do Cliente
					if (dadosCliente[4] != null) { // 4
						cliente.setCnpj((String) dadosCliente[4]);
					}

					// RG do Cliente
					if (dadosCliente[5] != null) { // 5
						cliente.setRg((String) dadosCliente[5]);
					}

					// Data de Emiss�o RG
					if (dadosCliente[6] != null) { // 6
						cliente.setDataEmissaoRg((Date) dadosCliente[6]);
					}

					// �rg�o Expedidor RG
					if (dadosCliente[7] != null) { // 7
						OrgaoExpedidorRg orgaoExpedidorRg = new OrgaoExpedidorRg();
						orgaoExpedidorRg.setDescricaoAbreviada((String) dadosCliente[7]);
						cliente.setOrgaoExpedidorRg(orgaoExpedidorRg);
					}

					// Unidade Federa��o
					if (dadosCliente[8] != null) { // 8
						UnidadeFederacao unidadeFederacao = new UnidadeFederacao();
						unidadeFederacao.setSigla((String) dadosCliente[8]);
						cliente.setUnidadeFederacao(unidadeFederacao);
					}

					// Data de Nascimento
					if (dadosCliente[9] != null) { // 9
						cliente.setDataNascimento((Date) dadosCliente[9]);
					}

					// Profiss�o
					if (dadosCliente[10] != null) { // 10
						Profissao profissao = new Profissao();
						profissao.setDescricao((String) dadosCliente[10]);
						cliente.setProfissao(profissao);
					}

					// Sexo
					if (dadosCliente[11] != null) { // 11
						PessoaSexo pessoaSexo = new PessoaSexo();
						pessoaSexo.setId((Integer) dadosCliente[11]);
						cliente.setPessoaSexo(pessoaSexo);
					}

					// Nome da M�e
					if (dadosCliente[12] != null) { // 12
						cliente.setNomeMae((String) dadosCliente[12]);
					}

					// Indicador de Uso
					if (dadosCliente[13] != null) { // 13
						cliente.setIndicadorUso((Short) dadosCliente[13]);
					}

					clienteEmitirBoletimCadastroHelper.setCliente(cliente);

					// Dados do Endere�o do Cliente
					// Tipo de Endere�o
					if (dadosCliente[14] != null) { // 14
						EnderecoTipo enderecoTipo = new EnderecoTipo();
						enderecoTipo.setId((Integer) dadosCliente[14]);
						clienteEndereco.setEnderecoTipo(enderecoTipo);
					}

					// Id do Logradouro
					if (dadosCliente[15] != null) { // 15
						Logradouro logradouro = new Logradouro();
						logradouro.setId((Integer) dadosCliente[15]);
						logradouroCep.setLogradouro(logradouro);
						logradouroBairro.setLogradouro(logradouro);
					}

					// Endere�o do Cliente
					FiltroClienteEndereco filtroClienteEndereco = new FiltroClienteEndereco();
					filtroClienteEndereco.adicionarParametro(new ParametroSimples(FiltroClienteEndereco.CLIENTE_ID, cliente.getId()));
					filtroClienteEndereco.adicionarParametro(new ParametroSimples(FiltroClienteEndereco.INDICADOR_CORRESPONDENCIA,
							ClienteEndereco.INDICADOR_ENDERECO_CORRESPONDENCIA));
					filtroClienteEndereco.adicionarCaminhoParaCarregamentoEntidade("logradouroCep.logradouro.logradouroTipo");
					filtroClienteEndereco.adicionarCaminhoParaCarregamentoEntidade("logradouroCep.logradouro.logradouroTitulo");
					filtroClienteEndereco.adicionarCaminhoParaCarregamentoEntidade("enderecoReferencia");
					filtroClienteEndereco.adicionarCaminhoParaCarregamentoEntidade("logradouroBairro.bairro.municipio.unidadeFederacao");
					filtroClienteEndereco.adicionarCaminhoParaCarregamentoEntidade("logradouroCep.cep");
					filtroClienteEndereco.adicionarCaminhoParaCarregamentoEntidade("logradouroCep");
					filtroClienteEndereco.adicionarCaminhoParaCarregamentoEntidade("enderecoTipo");

					Collection colecaoEnderecos = getControladorUtil().pesquisar(filtroClienteEndereco, ClienteEndereco.class.getName());

					if (colecaoEnderecos != null && !colecaoEnderecos.isEmpty()) {
						ClienteEndereco clienteEnderecoCorrespondencia = (ClienteEndereco) Util.retonarObjetoDeColecao(colecaoEnderecos);

						String endereco = clienteEnderecoCorrespondencia.getEnderecoFormatado();

						if (endereco != null && !endereco.trim().equals("")) {
							clienteEmitirBoletimCadastroHelper.setEnderecoFormatado(endereco);
						}
					}

					// CEP
					if (dadosCliente[16] != null) { // 16
						Cep cep = new Cep();
						cep.setCodigo((Integer) dadosCliente[16]);
						logradouroCep.setCep(cep);
					}

					// Id do Bairro
					if (dadosCliente[17] != null) { // 17
						Bairro bairro = new Bairro();
						bairro.setCodigo((Integer) dadosCliente[17]);

						Municipio municipio = new Municipio();
						municipio.setId((Integer) dadosCliente[25]);

						bairro.setMunicipio(municipio);

						logradouroBairro.setBairro(bairro);
					}

					// Endere�o de Refer�ncia
					if (dadosCliente[18] != null) { // 18
						EnderecoReferencia enderecoReferencia = new EnderecoReferencia();
						enderecoReferencia.setId((Integer) dadosCliente[18]);
						clienteEndereco.setEnderecoReferencia(enderecoReferencia);
					}

					// N�mero do Im�vel
					if (dadosCliente[19] != null) { // 19
						clienteEndereco.setNumero((String) dadosCliente[19]);
					}

					// Complemento
					if (dadosCliente[20] != null) { // 20
						clienteEndereco.setComplemento((String) dadosCliente[20]);
					}

					clienteEndereco.setLogradouroCep(logradouroCep);
					clienteEndereco.setLogradouroBairro(logradouroBairro);
					clienteEmitirBoletimCadastroHelper.setClienteEndereco(clienteEndereco);

					primeiroRegistro = false;

					// Tarifa Social Dado Economia
					FiltroClienteImovel filtroClienteImovel = new FiltroClienteImovel();
					filtroClienteImovel.adicionarParametro(new ParametroSimples(FiltroClienteImovel.CLIENTE_ID, cliente.getId()));
					filtroClienteImovel.adicionarCaminhoParaCarregamentoEntidade(FiltroClienteImovel.IMOVEL);

					Collection colecaoClienteImovel = getControladorUtil().pesquisar(filtroClienteImovel, ClienteImovel.class.getName());

					if (colecaoClienteImovel != null && !colecaoClienteImovel.isEmpty()) {
						ClienteImovel clienteImovel = (ClienteImovel) Util.retonarObjetoDeColecao(colecaoClienteImovel);

						FiltroTarifaSocialDadoEconomia filtroTarifaSocialDadoEconomia = new FiltroTarifaSocialDadoEconomia();
						filtroTarifaSocialDadoEconomia.adicionarParametro(new ParametroSimples(FiltroTarifaSocialDadoEconomia.IMOVEL_ID, clienteImovel
								.getImovel().getId()));
						filtroTarifaSocialDadoEconomia.adicionarCaminhoParaCarregamentoEntidade(FiltroTarifaSocialDadoEconomia.TARIFA_SOCIAL_CARTAO_TIPO);
						filtroTarifaSocialDadoEconomia.adicionarCaminhoParaCarregamentoEntidade(FiltroTarifaSocialDadoEconomia.RENDA_TIPO);

						Collection colecaoTarifaSocial = getControladorUtil().pesquisar(filtroTarifaSocialDadoEconomia,
								TarifaSocialDadoEconomia.class.getName());

						if (colecaoTarifaSocial != null && !colecaoTarifaSocial.isEmpty()) {
							TarifaSocialDadoEconomia tarifaSocialDadoEconomia = (TarifaSocialDadoEconomia) Util.retonarObjetoDeColecao(colecaoTarifaSocial);
							clienteEmitirBoletimCadastroHelper.setTarifaSocialDadoEconomia(tarifaSocialDadoEconomia);
						}

					}

				}

				ClienteFone clienteFone = new ClienteFone();

				// Dados do Telefone do Cliente
				// Tipo do Telefone
				if (dadosCliente[21] != null) { // 21
					FoneTipo foneTipo = new FoneTipo();
					foneTipo.setId((Integer) dadosCliente[21]);
					clienteFone.setFoneTipo(foneTipo);
				}

				// DDD
				if (dadosCliente[22] != null) { // 22
					clienteFone.setDdd((String) dadosCliente[22]);
				}

				// N�mero Telefone
				if (dadosCliente[23] != null) { // 23
					clienteFone.setTelefone((String) dadosCliente[23]);
				}

				// Ramal
				if (dadosCliente[24] != null) { // 24
					clienteFone.setRamal((String) dadosCliente[24]);
				}

				clientesFone.add(clienteFone);

			}

			clienteEmitirBoletimCadastroHelper.setClienteFone(clientesFone);

		}

		return clienteEmitirBoletimCadastroHelper;

	}

	/**
	 * 
	 * Usado pelo Filtrar Cliente Filtra o Cliente usando os paramentos
	 * informados
	 *
	 * @author Rafael Santos
	 * @date 27/11/2006
	 *
	 * @return
	 * @throws ErroRepositorioException
	 */
	public Collection filtrarCliente(String codigo, String cpf, String rg, String cnpj, String nome, String nomeMae, String cep, String idMunicipio,
			String idBairro, String idLogradouro, String indicadorUso, String tipoPesquisa, String tipoPesquisaNomeMae, String clienteTipo,
			String idEsferaPoder, Integer numeroPagina, String nis) throws ControladorException {

		Collection colecaoDadosCliente = null;
		Collection colecaoClientes = null;

		try {

			colecaoDadosCliente = this.repositorioCliente.filtrarCliente(codigo, cpf, rg, cnpj, nome, nomeMae, cep, idMunicipio, idBairro, idLogradouro,
					indicadorUso, tipoPesquisa, tipoPesquisaNomeMae, clienteTipo, idEsferaPoder, numeroPagina, nis);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

		if (colecaoDadosCliente != null && !colecaoDadosCliente.isEmpty()) {

			Iterator iteratorColecaoDadosCliente = colecaoDadosCliente.iterator();
			Cliente cliente = null;
			colecaoClientes = new ArrayList();

			while (iteratorColecaoDadosCliente.hasNext()) {

				cliente = new Cliente();

				Object[] array = (Object[]) iteratorColecaoDadosCliente.next();

				// codigo
				if (array[0] != null) {
					cliente.setId((Integer) array[0]);
				}

				// nome
				if (array[1] != null) {
					cliente.setNome((String) array[1]);
				}

				// rg
				if (array[2] != null) {
					cliente.setRg((String) array[2]);
				}

				// cpf
				if (array[3] != null) {
					cliente.setCpf((String) array[3]);
				}

				// cnpj
				if (array[4] != null) {
					cliente.setCnpj((String) array[4]);
				}

				ClienteTipo tipoCliente = null;
				// indicadorPessoaFisicaJuridica
				if (array[5] != null) {
					tipoCliente = new ClienteTipo();
					tipoCliente.setIndicadorPessoaFisicaJuridica((Short) array[5]);
					cliente.setClienteTipo(tipoCliente);
				}

				// descricao orgaoExpedidorRg
				if (array[6] != null) {
					OrgaoExpedidorRg orgaoExpedidorRg = new OrgaoExpedidorRg();
					orgaoExpedidorRg.setDescricao((String) array[6]);
					cliente.setOrgaoExpedidorRg(orgaoExpedidorRg);
				}

				// silga orgaoExpedidorRg
				if (array[7] != null) {
					UnidadeFederacao unidadeFederacao = new UnidadeFederacao();

					unidadeFederacao.setSigla((String) array[7]);
					cliente.setUnidadeFederacao(unidadeFederacao);
				}
				// descricao tipo cliente
				if (array[8] != null) {
					if (tipoCliente == null) {
						tipoCliente = new ClienteTipo();
					}
					tipoCliente.setDescricao((String) array[8]);
					cliente.setClienteTipo(tipoCliente);
				}

				// indicador uso
				if (array[9] != null) {
					cliente.setIndicadorUso((Short) array[9]);
				}

				colecaoClientes.add(cliente);
			}

		}

		return colecaoClientes;
	}

	/**
	 * 
	 * Usado pelo Filtrar Cliente Filtra a quantidade de Clientes usando os
	 * paramentos informados
	 *
	 * @author Rafael Santos
	 * @date 27/11/2006
	 *
	 * @return
	 * @throws ErroRepositorioException
	 */
	public Object filtrarQuantidadeCliente(String codigo, String cpf, String rg, String cnpj, String nome, String nomeMae, String cep, String idMunicipio,
			String idBairro, String idLogradouro, String indicadorUso, String tipoPesquisa, String tipoPesquisaNomeMae, String clienteTipo, String idEsferaPoder, String nis)
			throws ControladorException {

		Object quantidade = null;
		Integer retorno = null;

		try {
			quantidade = repositorioCliente.filtrarQuantidadeCliente(codigo, cpf, rg, cnpj, nome, nomeMae, cep, idMunicipio, idBairro, idLogradouro,
					indicadorUso, tipoPesquisa, tipoPesquisaNomeMae, clienteTipo, idEsferaPoder, nis);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

		if (quantidade != null) {
			retorno = (Integer) quantidade;

		}

		return retorno;
	}

	/**
	 * [UC0054] - Inserir Dados Tarifa Social
	 * 
	 * Pesquisa os Clientes Im�veis pelo id do Cliente, indicador de uso, motivo
	 * do fim da rela��o, pelo perfil do im�vel e pelo tipo da rela��o do
	 * cliente carregando o im�vel
	 * 
	 * Autor: Rafael Corr�a
	 * 
	 * Data: 27/12/2006
	 */
	public Collection pesquisarClienteImovelPeloClienteTarifaSocial(Integer idCliente) throws ControladorException {

		try {
			return repositorioClienteImovel.pesquisarClienteImovelPeloClienteTarifaSocial(idCliente);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}

	}

	/**
	 * [UC0054] - Inserir Dados Tarifa Social
	 * 
	 * Pesquisa os Clientes Im�veis pelo id do Im�vel carregando o im�vel, o
	 * cliente, o perfil do im�vel, o org�o expedidor do RG e a unidade da
	 * federa��o
	 * 
	 * Autor: Rafael Corr�a
	 * 
	 * Data: 27/12/2006
	 */
	public Collection pesquisarClienteImovelPeloImovelTarifaSocial(Integer idImovel) throws ControladorException {

		try {
			return repositorioClienteImovel.pesquisarClienteImovelPeloImovelTarifaSocial(idImovel);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}

	}

	/**
	 * [UC0054] - Inserir Dados Tarifa Social
	 * 
	 * Pesquisa os Clientes Im�veis pelo id do Im�vel carregando os dados
	 * necess�rios para retornar o seu endere�o
	 * 
	 * Autor: Rafael Corr�a
	 * 
	 * Data: 27/12/2006
	 */
	public Collection pesquisarClienteImovelPeloImovelParaEndereco(Integer idImovel) throws ControladorException {

		try {
			return repositorioClienteImovel.pesquisarClienteImovelPeloImovelParaEndereco(idImovel);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}

	}

	/**
	 * 
	 * Verifica se � usuario iquilino ou n�o
	 *
	 * @author S�vio Luiz
	 * @date 08/01/2007
	 *
	 * @param idImovel
	 * @return
	 * @throws ErroRepositorioException
	 */
	public boolean verificaUsuarioinquilino(Integer idImovel) throws ControladorException {
		Collection colecao = null;
		Integer idClienteUsuario = null;
		boolean naoInquilino = false;
		try {
			idClienteUsuario = repositorioClienteImovel.retornaIdClienteUsuario(idImovel);
			colecao = repositorioClienteImovel.retornaClientesRelacao(idImovel);
		} catch (ErroRepositorioException e) {
			throw new ControladorException("erro.sistema", e);
		}
		if (colecao != null && !colecao.isEmpty()) {
			Iterator iteParmsCliente = colecao.iterator();
			while (iteParmsCliente.hasNext()) {
				Integer idClienteBase = (Integer) iteParmsCliente.next();
				if (idClienteBase.equals(idClienteUsuario)) {
					naoInquilino = true;
					break;
				}

			}
		} else {
			naoInquilino = true;
		}
		return naoInquilino;
	}

	/**
	 * Atualiza logradouroCep de um ou mais im�veis
	 * 
	 * [UC0] Atualizar Logradouro
	 * 
	 * @author Raphael Rossiter
	 * @date 22/02/2007
	 * 
	 * @param
	 * @return void
	 */
	public void atualizarLogradouroCep(LogradouroCep logradouroCepAntigo, LogradouroCep logradouroCepNovo) throws ControladorException {

		try {

			this.repositorioClienteEndereco.atualizarLogradouroCep(logradouroCepAntigo, logradouroCepNovo);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * Atualiza logradouroBairro de um ou mais im�veis
	 * 
	 * [UC0] Atualizar Logradouro
	 * 
	 * @author Raphael Rossiter
	 * @date 22/02/2007
	 * 
	 * @param
	 * @return void
	 */
	public void atualizarLogradouroBairro(LogradouroBairro logradouroBairroAntigo, LogradouroBairro logradouroBairroNovo) throws ControladorException {

		try {

			this.repositorioClienteEndereco.atualizarLogradouroBairro(logradouroBairroAntigo, logradouroBairroNovo);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * [UC0544] Gerar Arwuivo Texto do Faturamento
	 * 
	 * Pesquisar ClienteImovel
	 * 
	 * @author Fl�vio Cordeiro
	 * @date 4/04/2006
	 * 
	 * 
	 * @return Colletion
	 * @throws ErroRepositorioException
	 */
	public Collection pesquisarClienteImovelGerarArquivoFaturamento() throws ControladorException {

		try {

			Collection clientes = new HashSet();

			Collection colecaoObjetos = this.repositorioClienteImovel.pesquisarClienteImovelGerarArquivoFaturamento();
			if (!colecaoObjetos.isEmpty()) {
				Iterator iterator = colecaoObjetos.iterator();
				while (iterator.hasNext()) {
					Object[] objeto = (Object[]) iterator.next();

					Cliente cliente = new Cliente();
					if (objeto[0] != null) {
						cliente.setId((Integer) objeto[0]);
					}
					if (objeto[1] != null) {
						cliente.setNome((String) objeto[1]);
					}
					clientes.add(cliente);
				}
			}
			return clientes;

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * [UC0864] Gerar Certid�o Negativa por Cliente
	 * 
	 * @author Rafael Corr�a
	 * @date 25/09/2008
	 * 
	 * @return
	 * @throws ControladorException
	 */
	public Collection<Integer> pesquisarClientesAssociadosResponsavel(Integer idCliente) throws ControladorException {

		try {

			return this.repositorioCliente.pesquisarClientesAssociadosResponsavel(idCliente);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	/**
	 * Pesquisa o rg do cliente do parcelamento a partir do idParcelamento
	 * Autor: Vivianne Sousa Data: 20/06/2007
	 */
	public Cliente pesquisarDadosClienteDoParcelamentoRelatorioParcelamento(Integer idParcelamento) throws ControladorException {

		Cliente cliente = null;
		Object[] dadosCliente = null;

		try {
			dadosCliente = this.repositorioClienteImovel.pesquisarDadosClienteDoParcelamentoRelatorioParcelamento(idParcelamento);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

		if (dadosCliente != null) {

			cliente = new Cliente();

			if (dadosCliente[0] != null) {
				// org�o expedidor
				OrgaoExpedidorRg orgaoExpedidorRg = new OrgaoExpedidorRg();
				orgaoExpedidorRg.setDescricao((String) dadosCliente[0]);
				cliente.setOrgaoExpedidorRg(orgaoExpedidorRg);
			}

			if (dadosCliente[1] != null) {
				// unidade federativa
				UnidadeFederacao unidadeFederacao = new UnidadeFederacao();
				unidadeFederacao.setSigla((String) dadosCliente[1]);
				cliente.setUnidadeFederacao(unidadeFederacao);
			}

			if (dadosCliente[2] != null) {
				// rg
				cliente.setRg((String) dadosCliente[2]);
			}

		}

		return cliente;
	}

	/**
	 * [UC0214] Efetuar Parcelamento de D�bitos
	 *
	 * @author Vivianne Sousa
	 * @date 27/07/2007
	 *
	 * @return
	 * @throws ErroRepositorioException
	 */
	public Cliente obterIdENomeCliente(String cpf) throws ControladorException {
		Object[] dadosCliente = null;
		Cliente cliente = null;

		try {

			dadosCliente = this.repositorioCliente.obterIdENomeCliente(cpf);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

		if (dadosCliente != null) {
			cliente = new Cliente();
			// id do Cliente
			if (dadosCliente[0] != null) {
				cliente.setId((Integer) dadosCliente[0]);
			}

			// nome de Cliente
			if (dadosCliente[1] != null) {
				cliente.setNome((String) dadosCliente[1]);
			}

		}

		return cliente;
	}

	/**
	 * [UC0214] Efetuar Parcelamento de D�bitos
	 *
	 * Alterado para registrar a transa��o na atualiza��o do CPF do cliente.
	 *
	 * @author Anderson Italo, Vivianne Sousa
	 * @date 11/08/2009, 30/07/2007
	 *
	 * @return
	 * @throws ErroRepositorioException
	 */
	public void atualizarCPFCliente(String cpf, Integer idCliente, Usuario usuarioLogado) throws ControladorException {

		try {

			String zeros = "";
			for (int a = 0; a < (11 - cpf.length()); a++) {
				zeros = zeros.concat("0");
			}
			cpf = zeros.concat(cpf);

			FiltroCliente filtroCliente = new FiltroCliente();
			filtroCliente.adicionarParametro(new ParametroSimples(FiltroCliente.ID, idCliente));

			Collection colecaoClientes = this.getControladorUtil().pesquisar(filtroCliente, Cliente.class.getName());

			Cliente cliente = (Cliente) Util.retonarObjetoDeColecao(colecaoClientes);

			cliente.setCpf(cpf);

			RegistradorOperacao registradorOperacao = new RegistradorOperacao(Operacao.OPERACAO_CLIENTE_ATUALIZAR, cliente.getId(), cliente.getId(),
					new UsuarioAcaoUsuarioHelper(usuarioLogado, UsuarioAcao.USUARIO_ACAO_EFETUOU_OPERACAO));

			registradorOperacao.registrarOperacao(cliente);
			String[] atributos = new String[1];
			atributos[0] = "cpf";
			getControladorTransacao().registrarTransacao(cliente, atributos);

			this.repositorioCliente.atualizarCPFCliente(cpf, idCliente);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public Cliente retornaClienteUsuario(Integer idImovel) throws ControladorException {
		try {
			return this.repositorioClienteImovel.retornaClienteUsuario(idImovel);
		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Cliente retornaClienteProprietario(Integer idImovel) throws ControladorException {
		try {
			return this.repositorioClienteImovel.retornaClienteProprietario(idImovel);
		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	public IClienteAtualizacaoCadastral obterClienteAtualizacaoCadastral(Integer idImovel, Short idClienteRelacaoTipo) throws ControladorException {

		try {
			IClienteAtualizacaoCadastral cliente = null;

			Object[] element = this.repositorioCliente.obterDadosCliente(idImovel, idClienteRelacaoTipo);

			if (element != null) {

				cliente = new ClienteAtualizacaoCadastral();

				cliente.setIdCliente((Integer) element[0]);

				cliente.setIdImovel(idImovel);

				if (element[1] != null) {
					cliente.setNome((String) element[1]);
				}

				if (element[2] != null) {
					cliente.setIdClienteTipo((Integer) element[2]);
				}

				if (element[3] != null) {
					cliente.setCpf((String) element[3]);
				} else if (element[4] != null) {
					cliente.setCpf((String) element[4]);
				}

				if (element[5] != null) {
					cliente.setRg((String) element[5]);
				}

				if (element[6] != null) {
					cliente.setDataEmissaoRg((Date) element[6]);
				}

				if (element[7] != null) {
					cliente.setDsAbreviadaOrgaoExpedidorRg((String) element[7]);
				}

				if (element[8] != null) {
					cliente.setDsUFSiglaOrgaoExpedidorRg((String) element[8]);
				}

				if (element[9] != null) {
					cliente.setDataNascimento((Date) element[9]);
				}

				// Profiss�o ou Ramo de Atividade
				if (element[25] != null) {
					if (((Short) element[25]).equals(ClienteTipo.INDICADOR_PESSOA_FISICA)) {
						if (element[10] != null) {
							cliente.setIdProfissao((Integer) element[10]);
						}
					} else {
						if (element[24] != null) {
							cliente.setIdRamoAtividade((Integer) element[24]);
						}
					}
				}

				if (element[11] != null) {
					cliente.getPessoaSexo().setId(((Integer) element[11]));
				}

				if (element[12] != null) {
					cliente.setNomeMae((String) element[12]);
				}

				if (element[13] != null) {
					cliente.setIndicadorUso((Short) element[13]);
				}

				if (element[14] != null) {
					cliente.setEmail((String) element[14]);
				}

				if (element[15] != null) {
					cliente.setIdEnderecoTipo((Integer) element[15]);
				}

				if (element[16] != null) {
					cliente.setIdLogradouro((Integer) element[16]);
				} else if (element[17] != null) {
					cliente.setIdLogradouro((Integer) element[17]);
				}

				// Logradouro
				Collection colecaoEndereco = getControladorEndereco().pesquisarLogradouroCliente((Integer) element[0]);
				if (colecaoEndereco != null && !colecaoEndereco.isEmpty()) {

					Iterator enderecoIterator = colecaoEndereco.iterator();

					Object[] arrayEndereco = (Object[]) enderecoIterator.next();

					String nome = (String) arrayEndereco[0];
					cliente.setDescricaoLogradouro(nome);

					if (arrayEndereco[3] != null) {
						Integer idTipo = (Integer) arrayEndereco[3];
						cliente.setIdLogradouroTipo(idTipo);
						String tipo = (String) arrayEndereco[1];
						cliente.setDsLogradouroTipo(tipo);
					}

					if (arrayEndereco[4] != null) {
						Integer idTitulo = (Integer) arrayEndereco[4];
						cliente.setIdLogradouroTitulo(idTitulo);
						String titulo = (String) arrayEndereco[2];
						cliente.setDsLogradouroTitulo(titulo);
					}

					if (arrayEndereco[5] != null) {
						Integer idMunicipio = (Integer) arrayEndereco[5];
						cliente.setIdMunicipio(idMunicipio);
						String nomeMunicipio = (String) arrayEndereco[6];
						cliente.setNomeMunicipio(nomeMunicipio);
					}

					if (arrayEndereco[7] != null) {
						Integer idUnidadeFederacao = (Integer) arrayEndereco[7];
						cliente.setIdUinidadeFederacao(idUnidadeFederacao);
						String dsUnidadeFederacao = (String) arrayEndereco[8];
						cliente.setDsUFSiglaMunicipio(dsUnidadeFederacao);
					}
				}

				// Cep
				if (element[18] != null) {
					cliente.setCodigoCep((Integer) element[18]);
				}

				// Bairro
				if (element[19] != null) {
					cliente.setIdBairro((Integer) element[19]);
				}

				// Descri��o do bairro
				if (element[20] != null) {
					cliente.setNomeBairro((String) element[20]);
				}

				// C�digo de refer�ncia
				if (element[21] != null) {
					cliente.setIdEnderecoReferencia((Integer) element[21]);
				}

				// N�mero do im�vel
				String numeroImovel = (String) element[22];
				if (numeroImovel != null && !numeroImovel.trim().equals("")) {
					cliente.setNumeroImovel(numeroImovel);
				}

				// Complemento do Im�vel
				String trunk = ((String) element[23]);

				if (trunk != null && trunk.length() > 25) {
					trunk = trunk.substring(0, 24);
				}

				if (element[23] != null) {
					cliente.setComplementoEndereco(trunk);
				}
				
				if(element[26] != null) {
					cliente.setNumeroNIS((String) element[26]);
				}

				cliente.setIdClienteRelacaoTipo(new Integer(idClienteRelacaoTipo));
			}
			return cliente;
		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection obterDadosClienteFone(Integer idCliente) throws ControladorException {
		try {
			return this.repositorioCliente.obterDadosClienteFone(idCliente);
		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Integer verificaExistenciaClienteAtualizacaoCadastral(Integer idCliente) throws ControladorException {
		try {
			return this.repositorioCliente.verificaExistenciaClienteAtualizacaoCadastral(idCliente);
		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection pesquisarClienteImovel(Integer idImovel) throws ControladorException {
		try {

			return repositorioClienteImovel.pesquisarClienteImovel(idImovel);

		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Integer pesquisarClienteResponsavelSuperiorParaPaginacaoCount(PesquisarClienteResponsavelSuperiorHelper helper) throws ControladorException {
		try {
			return this.repositorioCliente.pesquisarClienteResponsavelSuperiorParaPaginacaoCount(helper);
		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Collection<Cliente> pesquisarClienteResponsavelSuperiorParaPaginacao(PesquisarClienteResponsavelSuperiorHelper helper, Integer numeroPagina)
			throws ControladorException {

		try {
			return this.repositorioCliente.pesquisarClienteResponsavelSuperiorParaPaginacao(helper, numeroPagina);
		} catch (ErroRepositorioException ex) {
			ex.printStackTrace();
			sessionContext.setRollbackOnly();
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public IClienteAtualizacaoCadastral pesquisarClienteAtualizacaoCadastral(Integer idCliente, Integer idImovel, Integer idClienteRelacaoTipo)
			throws ControladorException {
		try {
			return repositorioCliente.pesquisarClienteAtualizacaoCadastral(idCliente, idImovel, idClienteRelacaoTipo);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Collection<ClienteFoneAtualizacaoCadastral> pesquisarClienteFoneAtualizacaoCadastral(Integer idCliente, Integer idMatricula, Integer idTipoFone,
			Integer idClienteRelacaoTipo, String numeroFone) throws ControladorException {
		try {
			return repositorioCliente.pesquisarClienteFoneAtualizacaoCadastral(idCliente, idMatricula, idTipoFone, idClienteRelacaoTipo, numeroFone);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public void atualizarIndicadorNomeContaClienteImovel(int idClienteImovel) throws ControladorException {

		try {
			repositorioClienteImovel.atualizarIndicadorNomeContaClienteImovel(idClienteImovel);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public void atualizarTelefonePadrao(String idCliente, String idClienteFonePadrao) throws ControladorException {

		try {
			repositorioCliente.atualizarTelefonePadrao(idCliente, idClienteFonePadrao);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public void removerTodosTelefonesPorCliente(Integer idCliente) throws ControladorException {
		try {
			repositorioCliente.removerTodosTelefonesPorCliente(idCliente);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Cliente retornaDadosClienteUsuario(Integer idImovel) throws ControladorException {
		try {
			return repositorioClienteImovel.retornaDadosClienteUsuario(idImovel);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public String obterNomeCliente(Integer idImovel) throws ControladorException {
		try {
			SistemaParametro sistemaParametro = this.getControladorUtil().pesquisarParametrosDoSistema();

			Cliente cliente = this.repositorioClienteImovel.retornaDadosClienteUsuario(idImovel);

			if (sistemaParametro.getIndicadorUsoNMCliReceitaFantasia().equals(ConstantesSistema.SIM)
					&& cliente.getIndicadorUsoNomeFantasiaConta().equals(ConstantesSistema.SIM) && cliente.getNomeAbreviado() != null) {

				return cliente.getNomeAbreviado();

			} else {
				return cliente.getNome();
			}

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public Cliente pesquisarClienteUsuarioDoImovel(Integer idImovel) throws ControladorException {
		try {
			return repositorioCliente.pesquisarClienteUsuarioDoImovel(idImovel);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection filtrarAutocompleteCliente(String valor) throws ControladorException {
		try {
			return repositorioCliente.filtrarAutocompleteCliente(valor);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection filtrarAutocompleteClienteResponsavel(String valor) throws ControladorException {
		try {
			return repositorioCliente.filtrarAutocompleteCliente(valor);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Integer pesquisarQtdClientesAssociadosResponsavelNaoJuridica(Integer idCliente) throws ControladorException {
		try {
			return repositorioCliente.pesquisarQtdClientesAssociadosResponsavelNaoJuridica(idCliente);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection pesquisarImoveisAssociadosCliente(Integer idCliente, Short relacaoTipo) throws ControladorException {
		try {
			return repositorioCliente.pesquisarImoveisAssociadosCliente(idCliente, relacaoTipo);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public String validarCliente(String cpfCliente, Integer matricula) throws ControladorException {
		try {
			return repositorioCliente.validarCliente(cpfCliente, matricula);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection obterClienteImovelporRelacaoTipo(Integer idImovel, Integer idRelacaoTipo) throws ControladorException {
		try {

			return repositorioCliente.obterClienteImovelporRelacaoTipo(idImovel, idRelacaoTipo);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Integer retornaIdClienteResponsavelIndicadorEnvioConta(Integer idImovel) throws ControladorException {
		try {

			return repositorioClienteImovel.retornaIdClienteResponsavelIndicadorEnvioConta(idImovel);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public Cliente pesquisarDadosCliente(Integer idCliente) throws ControladorException {
		try {

			return repositorioCliente.pesquisarDadosCliente(idCliente);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}

	}

	public ClienteImovel pesquisarClienteImovelOSFiscalizada(Integer idImovel) throws ControladorException {
		try {

			return repositorioClienteImovel.pesquisarClienteImovelOSFiscalizada(idImovel);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public ClienteTipo pesquisarClienteTipo(Integer idClienteTipo) throws ControladorException {
		try {
			return repositorioCliente.pesquisarClienteTipo(idClienteTipo);

		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Collection<Cliente> pesquisarClientePorCpfCnpj(String cpfCnpj) throws Exception {
		return repositorioCliente.pesquisarClientePorCpfCnpj(cpfCnpj);
	}

	public Short pesquisarIndicadorNegativacaoPeriodoClienteResponsavel(Integer idImovel, Integer idClienteRelacaoTipo) throws ControladorException {
		try {
			return repositorioClienteImovel.pesquisarIndicadorNegativacaoPeriodoClienteResponsavel(idImovel, idClienteRelacaoTipo);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public boolean existeEnderecoParaCliente(Integer idCliente) throws ControladorException {
		try {
			boolean retorno = false;
			Integer idEnderecoCliente = repositorioCliente.pesquisarEnderecoClienteParaNegativacao(idCliente);
			if (idEnderecoCliente != null) {
				retorno = true;
			}
			return retorno;
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Cliente pesquisarDadosClienteParaNegativacao(Integer idCliente, String cnpjEmpresa) throws ControladorException {
		try {
			return repositorioCliente.pesquisarDadosClienteParaNegativacao(idCliente, cnpjEmpresa);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public Localidade pesquisarLocalidadeCliente(Integer idCliente) throws ControladorException {
		try {
			return repositorioClienteEndereco.pesquisarLocalidadeCliente(idCliente);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}

	public String obterNomeClienteConta(Integer idImovel) throws ControladorException {
		try {
			return repositorioCliente.obterNomeClienteConta(idImovel);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}
	
	public Cliente obterUsuarioImovelPorData(Integer idImovel, Integer idClienteTipo, Date data) throws ControladorException {
		try {
			return repositorioCliente.obterUsuarioImovelPorData(idImovel, idClienteTipo, data);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}
	
	public List<Integer> pesquisarClientesPorCadastroUnico() throws ControladorException {
		try {
			return repositorioCliente.pesquisarClientesPorCadastroUnico();
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}
	
	public void atualizarNISCliente(Integer idCliente, Integer clie_icbolsafamilia) throws ControladorException {
		try {
			repositorioCliente.atualizarNISCliente(idCliente, clie_icbolsafamilia);
		} catch (ErroRepositorioException ex) {
			throw new ControladorException("erro.sistema", ex);
		}
	}
	
	public boolean verificarSeClientePossuiNis(Integer idCliente) throws ControladorException {
		Cliente cliente = this.pesquisarCliente(idCliente);
		return cliente.getNumeroNIS() != null;
	}

}
