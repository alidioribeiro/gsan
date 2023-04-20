package gcom.cadastro.atualizacaocadastral.validador;

import java.util.Map;

import gcom.cadastro.atualizacaocadastral.command.AtualizacaoCadastralImovel;
import gcom.util.ParserUtil;

public class ValidadorTamanhoLinhaImovelCommand extends ValidadorCommand {

	private ParserUtil parser;
	
	
	public ValidadorTamanhoLinhaImovelCommand(ParserUtil parser,
			AtualizacaoCadastralImovel cadastroImovel,
			Map<String, String> linha) {
		super(cadastroImovel, linha);
		this.parser = parser;
		
	}

	@Override
	public void execute() {
		if ((parser.getFonte().length() != 445 && parser.getFonte().length() > 754 ) || 
			(parser.getFonte().length() != 754 && parser.getFonte().length() < 445)) {
			cadastroImovel.addMensagemErroLayout("Linha Tipo 02 (Im�vel) n�o compat�vel com o Layout");
		}
	}
}
