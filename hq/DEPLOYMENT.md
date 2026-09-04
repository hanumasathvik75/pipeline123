# How to Host IgnitionAI HQ on the Web

IgnitionAI HQ is built on Next.js 16 with standard App Router architecture, zero native C-dependencies, and full compatibility with **Vercel**, **Render**, **Railway**, or any Node.js VPS.

---

## Method 1: Deploy to Vercel (Recommended — Free & 2 Minutes)

Vercel is the creator and native cloud for Next.js apps.

### Option A: Using GitHub (1-Click Automated CI/CD)
1. Push your repository to GitHub:
   ```bash
   git add .
   git commit -m "IgnitionAI HQ full release"
   git push origin main
   ```
2. Go to [vercel.com](https://vercel.com) and log in.
3. Click **Add New...** → **Project**.
4. Import your `ignitionai` repository.
5. In **Root Directory**, click *Edit* and select: `hq`.
6. Click **Deploy**.
7. In ~60 seconds, your site will be live at `https://your-app.vercel.app` (or your custom domain like `hq.ignitionai.in`).

### Option B: Deploying Directly from Terminal via Vercel CLI
Run the following inside the `hq` folder:
```powershell
cd c:\Users\SHASHANK\Downloads\ignitionai\hq
npx vercel
```
Follow the prompts (set project name e.g. `ignitionai-hq`), and Vercel will upload and deploy your build immediately to a live web URL!

---

## Method 2: Deploy to Railway / Render (Container / Node Server)

If you prefer a standalone server with persistent local disk storage:

1. **Build command**: `npm run build`
2. **Start command**: `npm run start`
3. **Port**: `3000`

---

## Connecting Supabase (Optional Remote PostgreSQL)

The app already includes the full database schema at [`supabase/schema.sql`](./supabase/schema.sql):
1. Create a free project at [supabase.com](https://supabase.com).
2. Go to the **SQL Editor**, paste the contents of `supabase/schema.sql`, and click **Run**.
3. In Vercel / your hosting provider's Environment Variables, add:
   - `NEXT_PUBLIC_SUPABASE_URL`
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY`
