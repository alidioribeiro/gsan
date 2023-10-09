package gcom.gui.cadastro.cliente;

import gcom.cadastro.cliente.Cliente;
import gcom.cadastro.cliente.FiltroCliente;
import gcom.fachada.Fachada;
import gcom.gui.ActionServletException;
import gcom.gui.GcomAction;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action para a pr�-exibi��o da p�gina de Manter Cliente
 * 
 * @author rodrigo
 */
public class ExibirManterClienteAction extends GcomAction {

	/**
	 * < <Descri��o do m�todo>>
	 * 
	 * @param actionMapping
	 *            Descri��o do par�metro
	 * @param actionForm
	 *            Descri��o do par�metro
	 * @param httpServletRequest
	 *            Descri��o do par�metro
	 * @param httpServletResponse
	 *            Descri��o do par�metro
	 * @return Descri��o do retorno
	 */
	public ActionForward execute(ActionMapping actionMapping,
			ActionForm actionForm, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {

		ActionForward retorno = actionMapping.findForward("manterCliente");

		Fachada fachada = Fachada.getInstancia();

		Collection<Cliente> clientes = null;

		//Mudar isso quando implementar a parte de seguran�a
		HttpSession sessao = httpServletRequest.getSession(false);
		
		// Parte da verifica��o do filtro
		FiltroCliente filtroCliente = null;
		
		String codigo  = (String) sessao.getAttribute("codigo");
		String cpf  = (String) sessao.getAttribute("cpf");
		String rg  = (String) sessao.getAttribute("rg");
		String cnpj  = (String) sessao.getAttribute("cnpj");
		String nome  = (String) sessao.getAttribute("nome");
		String nomeMae  = (String) sessao.getAttribute("nomeMae");		
		String cep  = (String) sessao.getAttribute("cep");
		String idMunicipio  = (String) sessao.getAttribute("idMunicipio");
		String codigoBairro  = (String) sessao.getAttribute("codigoBairro");
		String idLogradouro  = (String) sessao.getAttribute("idLogradouro");
		String indicadorUso  = (String) sessao.getAttribute("indicadorUso");
		String tipoPesquisa  = (String) sessao.getAttribute("tipoPesquisa");
		String tipoPesquisaNomeMae  = (String) sessao.getAttribute("tipoPesquisaNomeMae");
		String idEsferaPoder  = (String) sessao.getAttribute("idEsferaPoder");
		String nis = (String) sessao.getAttribute("nis");
		
		
		
		// Verifica se o filtro foi informado pela p�gina de filtragem de
		// cliente
		if (sessao.getAttribute("filtroCliente") != null ) {
			filtroCliente = (FiltroCliente) sessao
					.getAttribute("filtroCliente");
		} else {
			// Caso o exibirManterCliente n�o tenha passado por algum esquema de
			// filtro, a quantidade de registros � verificada para avaliar a necessidade
			// de filtragem
			filtroCliente = new FiltroCliente();
			
			retorno = actionMapping.findForward("filtrarCliente");
				
			// c�digo para checar ou nao o Atualizar
	        String primeiraVez = httpServletRequest.getParameter("menu");
			if (primeiraVez != null && !primeiraVez.equals("")) {
				//pesquisarActionForm.reset();
				//pesquisarActionForm.set("indicadorUso", "");
				sessao.setAttribute("indicadorAtualizar","1");
			}

			if (httpServletRequest.getParameter("desfazer") != null
	                && httpServletRequest.getParameter("desfazer").equalsIgnoreCase("S")) {
		        //Limpando o formulario
				//pesquisarActionForm.reset();
		        sessao.setAttribute("indicadorAtualizar","1");
	        }
	        
			sessao.removeAttribute("voltar");
			sessao.removeAttribute("idRegistroAtualizacao");	
		}

		// A pesquisa de clientes s� ser� feita se o forward estiver direcionado
		// para a p�gina de manterEmpresa
		if (retorno.getName().equalsIgnoreCase("manterCliente")) {
			
			sessao.removeAttribute("atualizar");
			
			// Seta a ordena��o desejada do filtro
			filtroCliente.setCampoOrderBy(FiltroCliente.NOME);

			filtroCliente
				.adicionarCaminhoParaCarregamentoEntidade("clienteTipo");
			/*filtroCliente
				.adicionarCaminhoParaCarregamentoEntidade(FiltroCliente.ORGAO_EXPEDIDOR_RG);*/
			filtroCliente
				.adicionarCaminhoParaCarregamentoEntidade(FiltroCliente.UNIDADE_FEDERACAO);

			// 1� Passo - Pegar o total de registros atrav�s de um count da consulta que aparecer� na tela
			//Integer totalRegistros = fachada
				///	.pesquisarClienteDadosClienteEnderecoCount(filtroCliente);
			Integer totalRegistros = (Integer) fachada.filtrarQuantidadeCliente(codigo,
					cpf,
					rg,
					cnpj,
					nome,
					nomeMae,		
					cep,
					idMunicipio,
					codigoBairro,
					idLogradouro,
					indicadorUso,
					tipoPesquisa,
					tipoPesquisaNomeMae, null,
					idEsferaPoder, nis);

			// 2� Passo - Chamar a fun��o de Pagina��o passando o total de registros
			retorno = this.controlarPaginacao(httpServletRequest, retorno,
					totalRegistros);

			// 3� Passo - Obter a cole��o da consulta que aparecer� na tela passando o numero de paginas
			// da pesquisa que est� no request
			//clientes = fachada
				//	.pesquisarClienteDadosClienteEndereco(filtroCliente, (Integer) httpServletRequest
					//		.getAttribute("numeroPaginasPesquisa"));
			clientes = fachada.filtrarCliente(
					codigo,
					cpf,
					rg,
					cnpj,
					nome,
					nomeMae,		
					cep,
					idMunicipio,
					codigoBairro,
					idLogradouro,
					indicadorUso,
					tipoPesquisa,
					tipoPesquisaNomeMae,
					null, idEsferaPoder,
					(Integer) httpServletRequest
							.getAttribute("numeroPaginasPesquisa"), nis);
		    
			if (clientes == null || clientes.isEmpty()) {
				// Nenhum cliente cadastrado
				throw new ActionServletException(
						"atencao.pesquisa.nenhumresultado");
			}

			
			if (clientes.size()== 1 && httpServletRequest.getAttribute("atualizar") != null
					&& (httpServletRequest.getParameter("page.offset") == null || httpServletRequest
							.getParameter("page.offset").equals("1"))){
				// caso o resultado do filtro s� retorne um registro 
				// e o check box Atualizar estiver selecionado
				//o sistema n�o exibe a tela de manter, exibe a de atualizar 
				retorno = actionMapping.findForward("atualizarCliente");
				Cliente cliente = (Cliente)clientes.iterator().next();
				httpServletRequest
                	.setAttribute("idRegistroAtualizacao", cliente.getId().toString());
				sessao
                	.setAttribute("atualizar","atualizar");
			}else{
				// A cole��o fica na sess�o devido ao esquema de pagina��o
				sessao.setAttribute("clientes", clientes);
			}
		}

		return retorno;
	}
}
