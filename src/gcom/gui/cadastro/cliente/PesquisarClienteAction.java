package gcom.gui.cadastro.cliente;

import gcom.cadastro.cliente.FiltroCliente;
import gcom.fachada.Fachada;
import gcom.gui.ActionServletException;
import gcom.gui.GcomAction;
import gcom.util.ConstantesSistema;
import gcom.util.filtro.ParametroSimples;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

/**
 * [UC0000] Pesquisar Cliente
 * Realiza a pesquisa de cliente de acordo com os par�metros informados
 * 
 * @author S�vio Luiz, Roberta Costa
 * @created 21/07/2005, 11/07/2006
 */
public class PesquisarClienteAction extends GcomAction {
	/**
	 * Description of the Method
	 * 
	 * @param actionMapping
	 *            Description of the Parameter
	 * @param actionForm
	 *            Description of the Parameter
	 * @param httpServletRequest
	 *            Description of the Parameter
	 * @param httpServletResponse
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	public ActionForward execute(ActionMapping actionMapping,
			ActionForm actionForm, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {

		ActionForward retorno = actionMapping.findForward("listaCliente");

		// Mudar isso quando tiver esquema de seguran�a
		HttpSession sessao = httpServletRequest.getSession(false);

		DynaValidatorForm pesquisarActionForm = (DynaValidatorForm) actionForm;

		// Obt�m a inst�ncia da Fachada
		Fachada fachada = Fachada.getInstancia();

		// Recupera os par�metros do form
		Integer idTipoCliente = (Integer) pesquisarActionForm
				.get("idTipoCliente");
		String nomeCliente = (String) pesquisarActionForm.get("nomeCliente");
		String cpf = (String) pesquisarActionForm.get("cpf");
		String rg = (String) pesquisarActionForm.get("rg");
		String cnpj = (String) pesquisarActionForm.get("cnpj");
		String cep = (String) pesquisarActionForm.get("cepClienteEndereco");
		String idMunicipio = (String) pesquisarActionForm.get("idMunicipioCliente");
		String codigoBairro = (String) pesquisarActionForm.get("codigoBairroCliente");
		String idLogradouro = (String) pesquisarActionForm.get("idLogradouroCliente");
		String idEsferaPoder = (String) pesquisarActionForm.get("idEsferaPoder");
		String tipoPesquisa = (String) pesquisarActionForm.get("tipoPesquisa");
		String nis = (String) pesquisarActionForm.get("nis");

		// filtro para a pesquisa de endereco do cliente
		/*FiltroCliente filtroCliente = new FiltroCliente();

		filtroCliente.setCampoOrderBy(FiltroCliente.NOME);
		filtroCliente
			.adicionarCaminhoParaCarregamentoEntidade(FiltroCliente.ORGAO_EXPEDIDOR_RG);
		filtroCliente
			.adicionarCaminhoParaCarregamentoEntidade(FiltroCliente.UNIDADE_FEDERACAO);
		filtroCliente
			.adicionarCaminhoParaCarregamentoEntidade("cliente");
		filtroCliente
			.adicionarCaminhoParaCarregamentoEntidade("cliente.clienteTipo");
		filtroCliente
			.adicionarCaminhoParaCarregamentoEntidade("clienteTipo.indicadorPessoaFisicaJuridica");
*/
		boolean peloMenosUmParametroInformado = false;

		// Insere os par�metros informados no filtro
		if (idTipoCliente != null
				&& idTipoCliente.intValue() != ConstantesSistema.NUMERO_NAO_INFORMADO) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimples(
//					FiltroCliente.TIPOCLIENTE_ID, new Integer(idTipoCliente)));
		}

		if (nomeCliente != null && !nomeCliente.trim().equalsIgnoreCase("")) {
            nomeCliente = nomeCliente.trim(); 
			peloMenosUmParametroInformado = true;
			if (tipoPesquisa != null
					&& tipoPesquisa
							.equals(ConstantesSistema.TIPO_PESQUISA_COMPLETA
									.toString())) {
//				filtroCliente.adicionarParametro(new ComparacaoTextoCompleto(
//						FiltroCliente.NOME, nomeCliente));
			} else {
//				filtroCliente.adicionarParametro(new ComparacaoTexto(
//						FiltroCliente.NOME, nomeCliente));
			}
		}

		if (cnpj != null && !cnpj.trim().equalsIgnoreCase("")) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimples(
//					FiltroCliente.CNPJ, cnpj));
		}

		if (cpf != null && !cpf.trim().equalsIgnoreCase("")) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimples(
//					FiltroCliente.CPF, cpf));
		}

		if (rg != null && !rg.trim().equalsIgnoreCase("")) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimples(
//					FiltroCliente.RG, rg));
		}

		if (cep != null && !cep.trim().equalsIgnoreCase("")) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimplesColecao(
//					FiltroCliente.CEP, cep));
		}

		if (idMunicipio != null && !idMunicipio.trim().equalsIgnoreCase("")) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimplesColecao(
//					FiltroCliente.MUNICIPIO_ID, new Integer(idMunicipio)));
		}

		if (codigoBairro != null && !codigoBairro.trim().equalsIgnoreCase("")) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimplesColecao(
//					FiltroCliente.BAIRRO_CODIGO, new Integer(codigoBairro)));
		}

		if (idLogradouro != null && !idLogradouro.trim().equalsIgnoreCase("")) {
			peloMenosUmParametroInformado = true;
//			filtroCliente.adicionarParametro(new ParametroSimplesColecao(
//					FiltroCliente.LOGRADOURO, new Integer(idLogradouro)));
		}
		
		if (idEsferaPoder != null && !idEsferaPoder.trim().equals("" + ConstantesSistema.NUMERO_NAO_INFORMADO)) {
			peloMenosUmParametroInformado = true;
		}
		
		if (nis != null && !nis.trim().equals("" + ConstantesSistema.NUMERO_NAO_INFORMADO)) {
			peloMenosUmParametroInformado = true;
		}

		// Erro caso o usu�rio mandou filtrar sem nenhum par�metro
		if (!peloMenosUmParametroInformado) {
			throw new ActionServletException(
					"atencao.filtro.nenhum_parametro_informado");
		}
		
		//filtroCliente.setCampoOrderBy(FiltroCliente.NOME);
		
		// 1� Passo - Pegar o total de registros atrav�s de um count da consulta que aparecer� na tela
		Integer totalRegistros = (Integer) fachada.filtrarQuantidadeCliente(null,
				cpf,
				rg,
				cnpj,
				nomeCliente,
				null,		
				cep,
				idMunicipio,
				codigoBairro,
				idLogradouro,
				null,
				tipoPesquisa,
				null,
				idTipoCliente.toString(), idEsferaPoder, nis);

		// 2� Passo - Chamar a fun��o de Pagina��o passando o total de registros
		retorno = this.controlarPaginacao(httpServletRequest, retorno,
				totalRegistros);

		// 3� Passo - Obter a cole��o da consulta que aparecer� na tela passando o numero de paginas
		// da pesquisa que est� no request
		Collection clientes = fachada.filtrarCliente(
				null,
				cpf,
				rg,
				cnpj,
				nomeCliente,
				null,		
				cep,
				idMunicipio,
				codigoBairro,
				idLogradouro,
				null,
				tipoPesquisa,
				null,
				idTipoCliente.toString(),
				idEsferaPoder,
				(Integer) httpServletRequest
						.getAttribute("numeroPaginasPesquisa"), nis);
		

/*		// 1� Passo - Pegar o total de registros atrav�s de um count da consulta que aparecer� na tela
		Integer totalRegistros = fachada
				.pesquisarClienteDadosClienteEnderecoCount(filtroCliente);

		// 2� Passo - Chamar a fun��o de Pagina��o passando o total de registros
		retorno = this.controlarPaginacao(httpServletRequest, retorno,
				totalRegistros);

		// 3� Passo - Obter a cole��o da consulta que aparecer� na tela passando o numero de paginas
		// da pesquisa que est� no request
		Collection clientes = fachada
				.pesquisarClienteDadosClienteEndereco(filtroCliente, (Integer) httpServletRequest
						.getAttribute("numeroPaginasPesquisa"));
*/
		if (clientes == null || clientes.isEmpty()) {
			// Nenhuma cliente cadastrado
			throw new ActionServletException(
					"atencao.pesquisa.nenhumresultado", null, "cliente");
		} else {
			// Coloca a cole��o na sess�o
			sessao.setAttribute("colecaoCliente",clientes);
		}
		
		//Coloca na sessao o parametro tipoConsulta
		String tipoConsulta = httpServletRequest.getParameter("tipoConsulta");
		if(tipoConsulta == null || tipoConsulta.equals("")){
			tipoConsulta = (String) sessao.getAttribute("tipoConsulta");
		}
		if(tipoConsulta != null && !tipoConsulta.equals("")){
			sessao.setAttribute("tipoConsulta", tipoConsulta);
		}else{
			sessao.removeAttribute("tipoConsulta");
		}

		return retorno;
	}

}
