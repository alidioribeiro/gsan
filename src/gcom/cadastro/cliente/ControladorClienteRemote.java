package gcom.cadastro.cliente;

import gcom.util.ControladorException;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * Declara��o p�blica de servi�os do Session Bean de ControladorCliente
 * 
 * @author S�vio Luiz
 * @created 25 de Abril de 2005
 */
public interface ControladorClienteRemote extends javax.ejb.EJBObject {

	/**
	 * Insere um cliente no sistema
	 * 
	 * @param cliente
	 *            Cliente a ser inserido
	 * @param telefones
	 *            Telefones do cliente
	 * @param enderecos
	 *            Endere�os do cliente
	 * @return Descri��o do retorno
	 * @exception RemoteException
	 *                Descri��o da exce��o
	 */
	public Integer inserirCliente(Cliente cliente, Collection telefones,
			Collection enderecos) throws RemoteException;

	/**
	 * < <Descri��o do m�todo>>
	 * 
	 * @param clienteImovel
	 *            Descri��o do par�metro
	 * @exception RemoteException
	 *                Descri��o da exce��o
	 */
	public void inserirClienteImovel(ClienteImovel clienteImovel)
			throws RemoteException;

	/**
	 * atualiza um cliente imovel economia com a data final da rela��o e o
	 * motivo.
	 * 
	 * @param clienteImovelEconomia
	 *            Description of the Parameter
	 * @exception RemoteException
	 *                Description of the Exception
	 */
	public void atualizarClienteImovelEconomia(
			Collection clientesImoveisEconomia) throws RemoteException;

	/**
	 * Atualiza um cliente no sistema
	 * 
	 * @param cliente
	 *            Cliente a ser atualizado
	 * @param telefones
	 *            Telefones do cliente
	 * @param enderecos
	 *            Endere�os do cliente
	 * @exception RemoteException
	 *                Descri��o da exce��o
	 */
	public void atualizarCliente(Cliente cliente, Collection telefones,
			Collection enderecos) throws RemoteException;
	
	/**
	 * Metodo que retorno todos os clinte do filtro passado
	 * 
	 * @param filtroCliente
	 *            Descri��o do par�metro
	 * @return Description of the Return Value
	 * @autor thiago toscano 
	 * @date 15/12/2005
	 * @throws ControladorException
	 */
	public Collection pesquisarCliente(FiltroCliente filtroCliente) throws ControladorException ;

	public boolean verificarSeClientePossuiNis(Integer idCliente) throws ControladorException;
}
