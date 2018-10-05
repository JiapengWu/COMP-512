// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------

package main.java.Server.Server.Common;

import java.io.Serializable;
import java.util.HashMap;

// Superclass for the three reservable items: Flight, Car, and Room
public abstract class ReservableItem extends RMItem implements Serializable
{
	private int m_nCount;
	private int m_nPrice;
	private int m_nReserved;
	private String m_location;
	protected HashMap<Integer, Integer> m_customers = new HashMap<Integer, Integer>();

	public ReservableItem(String location, int count, int price)
	{
		super();
		m_location = location;
		m_nCount = count;
		m_nPrice = price;
		m_nReserved = 0;
	}

	public void setCount(int count)
	{
		m_nCount = count;
	}

	public int getCount()
	{
		return m_nCount;
	}

	public void setPrice(int price)
	{
		m_nPrice = price;
	}

	public int getPrice()
	{
		return m_nPrice;
	}

	public void setReserved(int r)
	{
		m_nReserved = r;
	}

	public int getReserved(){
		return m_nReserved;
	}

	public String getLocation(){
		return m_location;
	}
	
	public void addReservation(int cid) {
		synchronized (m_customers) {
			Integer n_reservation = m_customers.get(cid);
			if(n_reservation != null) {
				m_customers.put(cid, n_reservation + 1);
			}
			else {
				m_customers.put(cid, 1);
			}
		}
	}
	
	public void cancelReservation(int cid, int cancels) throws Exception {
		synchronized (m_customers) {
			Integer n_reservation = m_customers.get(cid);
			if(n_reservation != null) {
				if(n_reservation - 1 == 0) {
					this.deleteCustomer(cid);
				}
				else {
					m_customers.put(cid, n_reservation - 1);
				}
			}
			else{
				throw new IllegalArgumentException("Customer " + cid + " has no reservation for item " + m_location);
			}
		}
	}
	

	public void deleteCustomer(int cid) {
		synchronized (m_customers) {
			m_customers.remove(cid);
		}
	}
	
	public String getSummaryInfo() {
		String location = this instanceof Flight?"Flight Number: ":"Location: ";
		synchronized (m_customers) {
			String info = this.getKey() + ":;Price: " + m_nPrice + ";" + location + m_location + ";";
			if(m_customers.isEmpty()) {
				info += "No reservations.;";
			}
			else {			
				for(Integer cid: m_customers.keySet()) {
					int n_reservation = m_customers.get(cid);
					info += "Customer " + cid + ": " + n_reservation + " revervations;";
				}
			}
			return info;
		}
	}
	
	public String toString(){
		return "RESERVABLEITEM key='" + getKey() + "', location='" + getLocation() +
			"', count='" + getCount() + "', price='" + getPrice() + "'";
	}

	public abstract String getKey();

	public Object clone()
	{
		ReservableItem obj = (ReservableItem)super.clone();
		obj.m_location = m_location;
		obj.m_nCount = m_nCount;
		obj.m_nPrice = m_nPrice;
		obj.m_nReserved = m_nReserved;
		return obj;
	}
}

