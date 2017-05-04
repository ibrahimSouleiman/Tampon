/* Copyright 2015 Pablo Arrighi, Sarah Boukris, Mehdi Chtiwi, 
   Michael Dubuis, Kevin Perrot, Julien Prudhomme.

   This file is part of SXP.

   SXP is free software: you can redistribute it and/or modify it 
   under the terms of the GNU Lesser General Public License as published 
   by the Free Software Foundation, version 3.

   SXP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
   without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
   PURPOSE.  See the GNU Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License along with SXP. 
   If not, see <http://www.gnu.org/licenses/>. */
package protocol.impl.sigma;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;

/*
 * TODO : Respect design pattern factory
 */
import crypt.impl.signatures.SigmaSignature;
import crypt.impl.signatures.SigmaSigner;

import controller.Application;
import controller.tools.JsonTools;
import controller.tools.LoggerUtilities;
import crypt.api.encryption.Encrypter;
import crypt.factories.EncrypterFactory;
import model.entity.ContractEntity;
import model.entity.ElGamalKey;
import model.entity.sigma.And;
import model.entity.sigma.Masks;
import model.entity.sigma.Or;
import model.entity.sigma.ResEncrypt;
import model.entity.sigma.ResponsesCCD;
import network.api.EstablisherService;
import network.api.Messages;
import network.api.ServiceListener;


/**
 * this class simulate the arbiter but in the end all users have this class
 * the arbiter can described message, and in the protocol CCD
 * @author sarah
 *
 */
public class Trent {
	
	protected final EstablisherService establisherService =(EstablisherService) Application.getInstance().getPeer().getService(EstablisherService.NAME);
	
	protected final ElGamalKey keys;
	private HashMap<Masks,BigInteger> eph = new HashMap<Masks, BigInteger>();
	
	private HashMap<String, TrentSolver> solvers = new HashMap<String, TrentSolver>();

	private Encrypter<ElGamalKey> encrypter;
	
	/**
	 * Constructor
	 */
	public  Trent(final ElGamalKey key){
		
		this.keys = key;
		
		encrypter = EncrypterFactory.createElGamalSerpentEncrypter();
		encrypter.setKey(keys);
		
		// Add a listener in case someone ask to resolve
		establisherService.addListener(new ServiceListener() {
			@Override
			public void notify(Messages messages) {// Finding the sender
				BigInteger msgSenKey = new BigInteger(messages.getMessage("sourceId"));
				ElGamalKey senderK = new ElGamalKey();
				senderK.setPublicKey(msgSenKey);
				senderK.setG(keys.getG());
				senderK.setP(keys.getP());
				
				String content = messages.getMessage("contract");
				
				resolve(content, senderK);
			}
		}, this.keys.getPublicKey().toString()+"TRENT");
		
	 }
	
	/*
	 * Trent resolve function
	 */
	private void resolve(String message, ElGamalKey senderK){
		JsonTools<String[]> json = new JsonTools<>(new TypeReference<String[]>(){});
		String[] content = json.toEntity(message);
		
		if (content != null){
			int round = Integer.parseInt(content[0]);
			
			JsonTools<HashMap<BigInteger, String>> json1 = new JsonTools<>(new TypeReference<HashMap<BigInteger, String>>(){});
			HashMap<BigInteger, String> uris = json1.toEntity(content[1]);
			
			JsonTools<ContractEntity> json2 = new JsonTools<>(new TypeReference<ContractEntity>(){});
			SigmaContract contract = new SigmaContract(json2.toEntity(content[2]));
			
			String m = new String(encrypter.decrypt(content[3].getBytes()));

			JsonTools<SigmaSignature> json4 = new JsonTools<>(new TypeReference<SigmaSignature>(){});
			String sign = new String(encrypter.decrypt(content[4].getBytes()));
			SigmaSignature signature = json4.toEntity(sign);
			
			
			SigmaSigner s = new SigmaSigner();
			s.setKey(this.keys);
			s.setReceiverK(senderK);
			s.setTrentK(this.keys);

			boolean verifiedOr = true;
			if (round > 0){
				byte[] data = (new String(contract.getHashableData()) + round).getBytes();
				JsonTools<Or[]> json3 = new JsonTools<>(new TypeReference<Or[]>(){});
				Or[] orT = json3.toEntity(m);
				
				// Checks the signature
				for (Or o : orT){
					verifiedOr = verifiedOr 
								&& o.Verifies(data) 
								&& this.VerifiesRes(o, senderK.getPublicKey());
				}
			}
			if (s.verify(m.getBytes(), signature) && verifiedOr){
				String id = new String(contract.getHashableData());
				if (solvers.get(id) == null){
					solvers.put(id, new TrentSolver(contract, this));
				}

				TrentSolver ts = solvers.get(id);
				ArrayList<String> resolved = ts.resolveT(m, round, senderK.getPublicKey().toString());

				if (resolved == null){
					establisherService.sendContract(new String(contract.getHashableData()), 
							senderK.getPublicKey().toString() + "TRENT",
							keys.getPublicKey().toString(),
							"Dishonest",
							uris.get(senderK));
				} else{
					SigmaSignature signa = s.sign(resolved.get(1).getBytes());
					JsonTools<SigmaSignature> jsona = new JsonTools<>(new TypeReference<SigmaSignature>(){});
					resolved.add(jsona.toJson(signa));
					
					JsonTools<ArrayList<String>> jsons = new JsonTools<>(new TypeReference<ArrayList<String>>(){});
					String answer = jsons.toJson(resolved);

					for (BigInteger k : uris.keySet()){
						establisherService.sendContract(new String(contract.getHashableData()), 
								k.toString()+"TRENT",
								keys.getPublicKey().toString(),
								answer,
								uris.get(k));
					}
				}
			}
		}
	}

	/**
	 * Create mask for the CCD response
	 * @param res
	 * @return Masks
	 */
	public Masks SendMasks(ResEncrypt res)
	{
		BigInteger s;
		s = Utils.rand(160, keys.getP());
		
		BigInteger a, aBis;
		
		a = keys.getG().modPow(s, keys.getP());
		aBis = res.getU().modPow(s, keys.getP());
		
		Masks masks = new Masks(a,aBis);
		eph.put(masks, s);
		
		return masks;
	}
	
	/**
	 * Create challenge for the not interactive version for the CCD
	 * @param mask
	 * @param message
	 * @return
	 */
	public BigInteger SendChallenge(Masks mask, byte[] message)
	{
		BigInteger challenge;
		byte[] buffer, resume;
		MessageDigest hash_function = null;
		
		String tmp = message.toString().concat(mask.getA().toString());
		
		buffer = tmp.getBytes();
		
		try {
			hash_function = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LoggerUtilities.logStackTrace(e);
		}
		
		resume = hash_function.digest(buffer);
		challenge = new BigInteger(resume);
		return challenge;
	}
	
	/**
	 * Create reponse CCD 
	 * @param challenge
	 * @param mask
	 * @return BigInteger
	 */
	public BigInteger SendAnswer(BigInteger challenge, Masks mask)
	{
		BigInteger r = (keys.getPrivateKey().multiply(challenge)).add(eph.get(mask));
		return r;	
	}

	/**
	 * Create response CCD will send
	 * @param resEncrypt
	 * @return
	 */
	public ResponsesCCD SendResponse(ResEncrypt resEncrypt)
	{		
		Masks mask = this.SendMasks(resEncrypt);
		BigInteger challenge = this.SendChallenge(mask, resEncrypt.getM());
		BigInteger response = this.SendAnswer(challenge, mask);
		
		return new ResponsesCCD(mask,challenge,response);
	}

	/**
	 * Create response CCD will send
	 * @param resEncrypt
	 * @return
	 */
	public ResponsesCCD SendResponse(ResEncrypt resEncrypt, byte[] m)
	{		
		Masks mask = this.SendMasks(resEncrypt);
		BigInteger challenge = this.SendChallenge(mask, m);
		BigInteger response = this.SendAnswer(challenge, mask);
		
		return new ResponsesCCD(mask,challenge,response);
	}
	
	public boolean VerifiesRes(Or o, BigInteger senPubK){
		boolean isVerified = false;
		for (And a : o.ands){
			byte[] data = Sender.getIdentificationData(a.rK.get(a.responses[0]));
			BigInteger k = new BigInteger(data);
			BigInteger h = decryption(a.resEncrypt);
			isVerified = isVerified || h.equals(k);
		}
		return isVerified;
	}
	
	/**
	 * decrypt
	 * @param cipherText
	 * @return
	 */
	public  byte[] decryption (byte[]cipherText)
	{
		ElGamal elGamal = new ElGamal (keys);
        return elGamal.decryptWithPrivateKey(cipherText);
	}
	
	public BigInteger decryption(ResEncrypt res){
		BigInteger u = res.getU();
		BigInteger v = res.getV();
		BigInteger p = keys.getP();
		BigInteger data = u.modPow(p.subtract(BigInteger.ONE).subtract(keys.getPrivateKey()), p).multiply(v).mod(p);
		return data;
	}
	
	/**
	 * gives trent public keys
	 * @return
	 */
	public ElGamalKey getKey(){
		ElGamalKey pubKey = new ElGamalKey();
		pubKey.setG(this.keys.getG());
		pubKey.setP(this.keys.getP());
		pubKey.setPublicKey(this.keys.getPublicKey());
		return pubKey;
	}
}
