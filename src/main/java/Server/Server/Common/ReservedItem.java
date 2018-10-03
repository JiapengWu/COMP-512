// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package main.java.Server.Server.Common;

import java.util.HashMap;

// Represents a customer's "reserved item" (e.g. Flight, Car, or Room)
// NOTE: if a customer reserves more than one item of the same kind, this is stored as a single
// instance of ReservedItem reflecting the *latest price*
public class ReservedItem extends RMItem
{
	private int m_nCount;
	private int m_nPrice;
	private String m_strReservableItemKey;
	private String m_strLocation;
	private HashMap<Integer, Integer> m_customers = new HashMap<Integer, Integer>();

	ReservedItem(String key, String location, int count, int price)
	{
		super();
		m_strReservableItemKey = key;
		m_strLocation = location;
		m_nCount = count;
		m_nPrice = price;
	}

	public String getReservableItemKey()
	{
		return m_strReservableItemKey;
	}

	public String getLocation()
	{
		return m_strLocation;
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
	
	public void deleteCustomer(int cid) {
		synchronized (m_customers) {
			m_customers.remove(cid);
		}
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<Integer, Integer> getReservedCustomers() {
		return (HashMap<Integer, Integer>) this.m_customers.clone();
	}
	
	public String getSummaryInfo() {
		String info = m_strReservableItemKey + ":;Price: " + m_nPrice + ";Location: " + m_strLocation + "Customer reservations :;";
		
		for(Integer cid: m_customers.keySet()) {
			int n_reservation = m_customers.get(cid);
			info += "Customer " + cid + ": " + n_reservation + "revervations;";
		}
		return info;
	}

	public String toString()
	{
		return "hashkey='" + getKey() + "', reservableItemKey='" + getReservableItemKey() +
			"', count='" + getCount() + "', price='" + getPrice() + "'";
	}

	// NOTE: hashKey is the same as the ReservableItem hashkey--this would have to change if we
	// weren't lumping all reservable items under the same price...
	public String getKey()
	{
		String s = getReservableItemKey();
		return s.toLowerCase();
	}
}

