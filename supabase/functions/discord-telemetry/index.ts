// Supabase Edge Function: discord-telemetry
// Secures Discord webhook relay without exposing secrets to the client.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const webhookUrl = Deno.env.get("DISCORD_WEBHOOK_URL");
    if (!webhookUrl) {
      console.warn("DISCORD_WEBHOOK_URL secret not configured in Supabase environment.");
      return new Response(JSON.stringify({ status: "skipped", message: "Webhook not configured" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    const body = await req.json();
    const {
      eventType,
      email,
      username,
      planName,
      method,
      licenseKey,
      amount,
      device,
      osVersion,
      transactionId,
      senderNumber,
      planRequested,
      requestId,
      reason,
      reviewer,
    } = body;

    let embed;
    const now = new Date().toISOString().replace("T", " ").substring(0, 19);

    if (eventType === "PAYMENT_SUBMITTED") {
      embed = {
        title: "💳 NEW MANUAL PAYMENT CLAIM SUBMITTED",
        description: "A customer has submitted a payment claim waiting for admin verification.",
        color: 0x3b82f6, // Blue
        fields: [
          { name: "User Email", value: email || "Unknown", inline: true },
          { name: "Method", value: method || "bKash/Nagad", inline: true },
          { name: "TrxID", value: transactionId || "N/A", inline: true },
          { name: "Sender No.", value: senderNumber || "N/A", inline: true },
          { name: "Amount", value: amount ? `${amount} BDT` : "N/A", inline: true },
          { name: "Plan Requested", value: planRequested || "PRO_MONTHLY", inline: true },
          { name: "Claim ID", value: requestId || "N/A", inline: false },
          { name: "Timestamp (UTC)", value: now, inline: true },
        ],
        footer: { text: "ASAR — Turn Time Into Progress | Payment Verification Queue" },
      };
    } else if (eventType === "PAYMENT_APPROVED") {
      embed = {
        title: "✅ PAYMENT CLAIM APPROVED — PRO ACTIVATED",
        description: `Payment claim has been approved and subscription activated.`,
        color: 0x10b981, // Emerald Green
        fields: [
          { name: "User Email / ID", value: email || "Unknown", inline: true },
          { name: "Plan Activated", value: planRequested || planName || "PRO", inline: true },
          { name: "TrxID", value: transactionId || "N/A", inline: true },
          { name: "Approved By", value: reviewer || "Admin", inline: true },
          { name: "Timestamp (UTC)", value: now, inline: true },
        ],
        footer: { text: "ASAR — Turn Time Into Progress | Subscription Activated" },
      };
    } else if (eventType === "PAYMENT_REJECTED") {
      embed = {
        title: "❌ PAYMENT CLAIM REJECTED",
        description: `Payment claim was reviewed and rejected.`,
        color: 0xef4444, // Red
        fields: [
          { name: "User Email / ID", value: email || "Unknown", inline: true },
          { name: "TrxID", value: transactionId || "N/A", inline: true },
          { name: "Reason", value: reason || "Invalid transaction details", inline: false },
          { name: "Rejected By", value: reviewer || "Admin", inline: true },
          { name: "Timestamp (UTC)", value: now, inline: true },
        ],
        footer: { text: "ASAR — Turn Time Into Progress | Payment Rejected" },
      };
    } else if (eventType === "PRO_PURCHASE" || eventType === "LICENSE_REDEMPTION") {
      embed = {
        title: "💎 NEW PRO ACTIVATION / PAYMENT CONFIRMED!",
        description: "A customer has successfully purchased or activated **ASAR Pro**.",
        color: 0xf59e0b, // Amber gold
        fields: [
          { name: "Plan Tier", value: planName || "Pro", inline: true },
          { name: "Payment Method", value: method || "Web License", inline: true },
          ...(amount ? [{ name: "Amount", value: `${amount}`, inline: true }] : []),
          ...(licenseKey ? [{ name: "License Key", value: licenseKey, inline: true }] : []),
          { name: "Timestamp (UTC)", value: now, inline: true },
        ],
        footer: { text: "ASAR — Turn Time Into Progress | Revenue" },
      };
    } else {
      const isNewUser = eventType === "SIGN_UP" || eventType === "ACCOUNT_CREATED";
      embed = {
        title: isNewUser ? "🎉 New User Registered!" : "👤 User Logged In",
        description: "User authenticated in ASAR Focus app.",
        color: isNewUser ? 0x10b981 : 0x38bdf8,
        fields: [
          { name: "Email", value: email || "Anonymous", inline: true },
          ...(username ? [{ name: "Username", value: username, inline: true }] : []),
          { name: "Auth Event", value: eventType || "SIGN_IN", inline: true },
          ...(device ? [{ name: "Device", value: device, inline: true }] : []),
          ...(osVersion ? [{ name: "OS Version", value: osVersion, inline: true }] : []),
          { name: "Timestamp (UTC)", value: now, inline: true },
        ],
        footer: { text: "ASAR — Turn Time Into Progress | Auth" },
      };
    }

    const discordPayload = {
      username: "ASAR Telemetry",
      embeds: [embed],
    };

    const discordRes = await fetch(webhookUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(discordPayload),
    });

    if (!discordRes.ok) {
      console.error(`Discord responded with error: ${discordRes.status}`);
      return new Response(JSON.stringify({ status: "error", code: discordRes.status }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    return new Response(JSON.stringify({ status: "success" }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });
  } catch (err) {
    console.error("Telemetry error:", err);
    return new Response(JSON.stringify({ error: err.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});
