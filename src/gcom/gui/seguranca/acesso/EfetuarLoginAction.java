package gcom.gui.seguranca.acesso;

import gcom.fachada.Fachada;
import gcom.gui.ActionServletException;
import gcom.gui.GcomAction;
import gcom.gui.SessaoHttpListener;
import gcom.seguranca.acesso.FiltroGrupoFuncionalidadeOperacao;
import gcom.seguranca.acesso.Funcionalidade;
import gcom.seguranca.acesso.FuncionalidadeCategoria;
import gcom.seguranca.acesso.Grupo;
import gcom.seguranca.acesso.GrupoFuncionalidadeOperacao;
import gcom.seguranca.acesso.GrupoFuncionalidadeOperacaoPK;
import gcom.seguranca.acesso.usuario.FiltroUsuario;
import gcom.seguranca.acesso.usuario.FiltroUsuarioGrupoRestricao;
import gcom.seguranca.acesso.usuario.Usuario;
import gcom.seguranca.acesso.usuario.UsuarioFavorito;
import gcom.seguranca.acesso.usuario.UsuarioGrupoRestricao;
import gcom.seguranca.acesso.usuario.UsuarioSituacao;
import gcom.util.ConstantesSistema;
import gcom.util.Internacionalizador;
import gcom.util.Util;
import gcom.util.filtro.FiltroParametro;
import gcom.util.filtro.ParametroSimples;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Esse action valida o usu�rio e coloca as informa��es na sess�o, todo o acesso
 * ter� que passar obrigatoriamente por aqui primeiro
 * 
 * @author Pedro Alexandre
 * @date 05/07/2006
 */
public class EfetuarLoginAction extends GcomAction {

	/**
	 * [UC0287] - Efetuar Login
	 * 
	 * @author Pedro Alexandre
	 * @date 04/07/2006
	 * 
	 * @param actionMapping
	 * @param actionForm
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 */
	public ActionForward execute(ActionMapping actionMapping, ActionForm actionForm,
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

		// Prepara o retorno da a��o para a tela principal
		ActionForward retorno = actionMapping.findForward("telaPrincipal");

		// Recupera o ActionForm
		EfetuarLoginActionForm loginActionForm = (EfetuarLoginActionForm) actionForm;

		String visualizacaoRAUrgencia = (String) httpServletRequest.getParameter("visualizacaoRAUrgencia");

		if (visualizacaoRAUrgencia == null || !visualizacaoRAUrgencia.equals("sim")) {

			// Vari�vel que vai armazenar o usu�rio logado
			Usuario usuarioLogado = null;

			// Recupera o login e a senha do usu�rio
			String login = loginActionForm.getLogin();
			String senha = loginActionForm.getSenha();

			// [FS0003] - Verificar preenchimento do login
			if (login == null || login.trim().equals("")) {
				this.reportarErros(httpServletRequest, "atencao.login.invalido");
				retorno = actionMapping.findForward("telaLogin");
			} else {

				// Cria a vari�vel que vai armazenar a mensagem com a quantidade de
				// dias que falta para expirar a validade da senha
				String mensagemExpiracao = "";

				// [FS0001] - Verificar exist�ncia do login
				if (!this.verificarExistenciaLogin(login)) {
					this.reportarErrosMensagem(httpServletRequest, "atencao.login.inexistente", login);
					retorno = actionMapping.findForward("telaLogin");
				} else {

					// Cria uma instancia da sess�o
					HttpSession sessao = httpServletRequest.getSession(true);

					// [FS0004] - Validar senha do login
					// Busca o usu�rio no sistema, o usu�rio ser� nulo se n�o
					// existir
					usuarioLogado = this.getFachada().validarUsuario(login, senha);

					// [FS0005] - Verificar n�mero de tentativas.
					Integer numeroTentativas = (Integer) sessao.getAttribute("numeroTentativas");
					Short numeroTentativasPermitidas = this.getSistemaParametro().getNumeroMaximoLoginFalho();
					if (numeroTentativas == null) {
						numeroTentativas = new Integer(0);
						sessao.setAttribute("numeroTentativas", numeroTentativas);
					}

					// Recupera o login do usu�rio da sess�o
					String loginUsuarioSessao = (String) sessao.getAttribute("loginUsuarioSessao");

					// Caso seja a primeira vez que o usu�rio esteja logando
					// joga o login do usu�rio na sess�o
					if (loginUsuarioSessao == null) {
						loginUsuarioSessao = login;
						sessao.setAttribute("loginUsuarioSessao", loginUsuarioSessao);
					}

					// Caso o usu�rio n�o esteja cadastrado, manda o erro para a
					// p�gina de login
					if (usuarioLogado == null) {
						this.reportarErros(httpServletRequest, "atencao.usuario.inexistente");
						retorno = actionMapping.findForward("telaLogin");

						/*
						 * Caso o login informado seja igual ao que est� na sess�o incrementa o n� de
						 * tentativas e joga esse n� na sess�o verifica se o n� de tentativas � maior
						 * que a permitida se for bloqueia a senha do usu�rio e indica o erro na p�gina
						 * de login
						 */
						if (loginUsuarioSessao.equals(login)) {
							numeroTentativas = numeroTentativas + 1;
							sessao.setAttribute("numeroTentativas", numeroTentativas);

							// [FS0005] - Verificar n�mero de tentativas de acesso
							if (numeroTentativas.intValue() > numeroTentativasPermitidas.intValue()) {
								this.bloquearSenha(login);
								this.reportarErros(httpServletRequest, "atencao.usuario.senha.bloqueada");
								retorno = actionMapping.findForward("telaLogin");
							}
						} else {
							// Zera o n� de tentativas de acesso e joga o login do usu�rio na sess�o
							numeroTentativas = 0;
							sessao.setAttribute("loginUsuarioSessao", login);
						}

					} else {
						// [FS0002] - Verificar situa��o do usu�rio
						if (!this.verificarSituacaoUsuario(usuarioLogado)) {
							if (usuarioLogado.getUsuarioSituacao().getId().equals(UsuarioSituacao.INATIVO)) {
								throw new ActionServletException("atencao.usuario_invalido", null,
										usuarioLogado.getLogin());
							} else {
								this.reportarErrosMensagem(httpServletRequest, "atencao.usuario.situacao.invalida",
										login + " est� com situa��o correspondente a "
												+ usuarioLogado.getUsuarioSituacao().getDescricaoAbreviada());
								retorno = actionMapping.findForward("telaLogin");
							}
						}

						// [SB0005] Efetuar Controle de Altera��o de Senha
						boolean disponibilizarAlteracaoSenha = false;
						Date dataExpiracaoAcesso = usuarioLogado.getDataExpiracaoAcesso();
						UsuarioSituacao usuarioSituacao = usuarioLogado.getUsuarioSituacao();

						// Caso a data de expira��o de acesso esteja preenchida e seja menor
						// que a data atual disponibiliza a tela de altera��o de senha
						if (dataExpiracaoAcesso != null) {
							if (dataExpiracaoAcesso.before(new Date())) {
								disponibilizarAlteracaoSenha = true;
							}
						}

						// Caso a situa��o da senha do usu�rio seja igual a "pendente"
						// disponibiliza a tela de altera��o de senha
						if (usuarioSituacao.getId().equals(UsuarioSituacao.PENDENTE_SENHA)) {
							disponibilizarAlteracaoSenha = true;
						}

						// Caso a flag de disponibilizar altera��o de senha esteja "true"
						// seta o mapeamento para a tela de alterar senha
						sessao.setAttribute("usuarioLogado", usuarioLogado);

						if (disponibilizarAlteracaoSenha) {
							retorno = actionMapping.findForward("alterarSenha");
						}

						Fachada.getInstancia().montarMenuUsuario(sessao, httpServletRequest.getRemoteAddr());
					}
				}
			}

		} else {
			Usuario usuarioLogado = (Usuario) this.getSessao(httpServletRequest).getAttribute("usuarioLogado");

			// C�digo para remover a mensagem de "Alerta de RA Urgente" quando usuario
			// pressionar OK
			this.getFachada().atualizarUsuarioVisualizacaoRaUrgencia(null, null, usuarioLogado.getId(), null, 1);

			this.getSessao(httpServletRequest).setAttribute("RAUrgencia", "false");

		}

		verificarLocaleInternacionalizacao(httpServletRequest);

		return retorno;
	}

	private void verificarLocaleInternacionalizacao(HttpServletRequest httpServletRequest) {
		Locale localeStruts = (Locale) httpServletRequest.getSession(false).getAttribute(Globals.LOCALE_KEY);

		if (Internacionalizador.getLocale() == null || !Internacionalizador.getLocale().equals(localeStruts)) {

			Internacionalizador.setLocale(localeStruts);
		}
	}

	/**
	 * Verifica se o login informado existe para algum usu�rio do sistema retorna
	 * true se existir caso contr�rio retorna false.
	 * 
	 * [UC0287] - Verificar exist�ncia do login
	 * 
	 * @author Pedro Alexandre
	 * @date 06/07/2006
	 * 
	 * @param login
	 * @return
	 */
	private boolean verificarExistenciaLogin(String login) {
		// Inicializa o retorno para falso(login n�o existe)
		boolean retorno = false;

		// Cria o filtro e pesquisa o usu�rio com o login informado
		FiltroUsuario filtroUsuario = new FiltroUsuario();
		filtroUsuario.adicionarParametro(new ParametroSimples(FiltroUsuario.LOGIN, login));
		Collection usuarios = Fachada.getInstancia().pesquisar(filtroUsuario, Usuario.class.getName());

		// Caso exista o usu�rio com o login informado
		// seta o retorno para verdadeiro(login existe no sistema)
		if (usuarios != null && !usuarios.isEmpty()) {
			retorno = true;
		}
		// Retorna um indicador se o login informado existe ou n�o no sistema
		return retorno;
	}

	/**
	 * Met�do que verifica se a situa��o do usu�rio � diferente de ativo ou se �
	 * igual a senha pendente.Caso seja uma ou outra situa��o levanta uma exce��o
	 * para o usu�rio indicando que o usu�rio n�o pode se logar ao sistema.
	 * 
	 * [FS0002] - Verificar situa��o do usu�rio
	 * 
	 * @author Pedro Alexandre
	 * @date 06/07/2006
	 * 
	 * @param usuarioLogado
	 * @return
	 */
	private boolean verificarSituacaoUsuario(Usuario usuarioLogado) {
		boolean retorno = true;
		// Recupera a situa��o do usu�rio
		UsuarioSituacao usuarioSituacao = usuarioLogado.getUsuarioSituacao();

		/*
		 * Caso a situa��o do usu�rio n�o seja igual a ativo ou seja igual a pendente
		 * retorna uma flag indicando que o usu�rio n�o pode acessar o sistema
		 */
		if ((!usuarioSituacao.getId().equals(UsuarioSituacao.ATIVO))
				&& (!usuarioSituacao.getId().equals(UsuarioSituacao.PENDENTE_SENHA))) {
			retorno = false;
		}

		// Retorna uma flag indicando se a situ��o do usu�rio permite o acesso
		// ao sistema
		return retorno;
	}

	/**
	 * Bloqueia a senha do usu�rio depois de o n�meros de tentativas de acesso
	 * exceder o n�mero m�ximo de tentativas permitidas
	 * 
	 * [FS0005] - Verificar n�mero de tentativas de acesso
	 * 
	 * @author Pedro Alexandre
	 * @date 06/07/2006
	 * 
	 * @param usuarioLogado
	 */
	private void bloquearSenha(String login) {

		// Pesquisa o us�rio que vai ser bloqueada sua senha
		FiltroUsuario filtroUsuario = new FiltroUsuario();
		filtroUsuario.adicionarParametro(new ParametroSimples(FiltroUsuario.LOGIN, login));
		Collection usuarios = Fachada.getInstancia().pesquisar(filtroUsuario, Usuario.class.getName());

		// Caso encontre o usu�rio com o login informado
		if (usuarios != null && !usuarios.isEmpty()) {
			// Recupera o usu�rio
			Usuario usuarioLogado = (Usuario) usuarios.iterator().next();

			// Atualiza a situa��o do usu�rio para bloqueada
			UsuarioSituacao usuarioSituacao = new UsuarioSituacao();
			usuarioSituacao.setId(UsuarioSituacao.SENHA_BLOQUEADA);

			// Recupera o n� de vezes que o usu�rio foi bloqueado
			Short bloqueioAcesso = usuarioLogado.getBloqueioAcesso();

			/*
			 * Caso o usu�rio nunca tenha sido bloqueado seta o n� de bloqueios para 1(um)
			 * caso contr�rio incrementa o valor do n� de bloqueio do usu�rio
			 */
			if (bloqueioAcesso == null) {
				usuarioLogado.setBloqueioAcesso(new Short("1"));
			} else {
				usuarioLogado.setBloqueioAcesso((new Integer(usuarioLogado.getBloqueioAcesso() + 1)).shortValue());
			}

			// Atualiza os dados do usu�rio
			usuarioLogado.setUsuarioSituacao(usuarioSituacao);
			usuarioLogado.setUltimaAlteracao(new Date());
			Fachada.getInstancia().atualizar(usuarioLogado);
		}
	}
}
